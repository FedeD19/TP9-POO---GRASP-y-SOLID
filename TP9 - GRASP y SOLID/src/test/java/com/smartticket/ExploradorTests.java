package com.smartticket;

import com.smartticket.model.Cliente;
import com.smartticket.model.Entrada;
import com.smartticket.model.EstadoEntrada;
import com.smartticket.model.EstadoVenta;
import com.smartticket.model.Evento;
import com.smartticket.model.Lugar;
import com.smartticket.model.Sector;
import com.smartticket.model.Venta;
import com.smartticket.repository.ClienteRepository;
import com.smartticket.repository.EntradaRepository;
import com.smartticket.repository.EventoRepository;
import com.smartticket.repository.LugarRepository;
import com.smartticket.repository.SectorRepository;
import com.smartticket.repository.VentaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas de consulta: la transaccion de cada test se revierte al terminar. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExploradorTests {
    @Autowired private MockMvc mvc;
    @Autowired private ClienteRepository clientes;
    @Autowired private LugarRepository lugares;
    @Autowired private SectorRepository sectores;
    @Autowired private EventoRepository eventos;
    @Autowired private EntradaRepository entradas;
    @Autowired private VentaRepository ventas;

    private record Escenario(Evento evento, Sector sector, Cliente cliente, Venta venta, Entrada entrada) {}

    private Escenario crearEscenario(String sufijo, boolean comprada) {
        Lugar lugar = new Lugar();
        lugar.setNombre("Auditorio " + sufijo);
        lugar.setDireccion("Av. Test 100");
        lugar = lugares.saveAndFlush(lugar);

        Sector sector = new Sector();
        sector.setNombre("Platea " + sufijo);
        sector.setCapacidadMaxima(4);
        sector.setLugar(lugar);
        sector = sectores.saveAndFlush(sector);

        Evento evento = new Evento();
        evento.setNombre("Evento " + sufijo);
        evento.setFechaHora(LocalDateTime.of(2026, 12, 20, 21, 0));
        evento.setLugar(lugar);
        evento = eventos.saveAndFlush(evento);

        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente " + sufijo);
        cliente.setEmail("explorador-" + sufijo + "@example.com");
        cliente = clientes.saveAndFlush(cliente);

        Venta venta = null;
        if (comprada) {
            venta = new Venta();
            venta.setCliente(cliente);
            venta.setEstado(EstadoVenta.PAGADA);
            venta.setTotal(new BigDecimal("12000.00"));
            venta = ventas.saveAndFlush(venta);
        }

        Entrada entrada = new Entrada();
        entrada.setEvento(evento);
        entrada.setSector(sector);
        entrada.setCodigoQR("qr-secreto-" + sufijo);
        entrada.setEstado(comprada ? EstadoEntrada.EMITIDA : EstadoEntrada.DISPONIBLE);
        entrada.setVenta(venta);
        entrada = entradas.saveAndFlush(entrada);
        return new Escenario(evento, sector, cliente, venta, entrada);
    }

    @Test
    void entradasSeListanPaginadasSinExponerElCodigoDeAcceso() throws Exception {
        Escenario caso = crearEscenario("pagina", true);
        String respuesta = mvc.perform(get("/api/v1/explorador/entradas")
                .param("eventoId", String.valueOf(caso.evento().getId()))
                .param("pagina", "0").param("tamano", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pagina").value(0))
            .andExpect(jsonPath("$.tamano").value(1))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].id").value(caso.entrada().getId().intValue()))
            .andExpect(jsonPath("$.contenido[0].estado").value("EMITIDA"))
            .andExpect(jsonPath("$.contenido[0].ventaId").value(caso.venta().getId().intValue()))
            .andReturn().getResponse().getContentAsString();
        assertFalse(respuesta.contains("codigoQR"));
        assertFalse(respuesta.contains("qr-secreto-pagina"));
        assertEquals(EstadoEntrada.EMITIDA, entradas.findById(caso.entrada().getId()).orElseThrow().getEstado());
    }

    @Test
    void entradasPuedenFiltrarsePorEvento() throws Exception {
        Escenario primero = crearEscenario("filtro-a", false);
        Escenario segundo = crearEscenario("filtro-b", false);
        mvc.perform(get("/api/v1/explorador/entradas")
                .param("eventoId", primero.evento().getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].eventoId").value(primero.evento().getId().intValue()));
        mvc.perform(get("/api/v1/explorador/entradas")
                .param("eventoId", segundo.evento().getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].eventoId").value(segundo.evento().getId().intValue()));
    }

    @Test
    void unaPaginaFueraDeRangoDevuelveColeccionVacia() throws Exception {
        Escenario caso = crearEscenario("sin-datos", false);
        mvc.perform(get("/api/v1/explorador/entradas")
                .param("eventoId", caso.evento().getId().toString())
                .param("pagina", "2").param("tamano", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido").isEmpty());
    }

    @Test
    void ventasIncluyenClienteTotalYCantidadPeroNoQrNiTarjeta() throws Exception {
        Escenario caso = crearEscenario("venta", true);
        String respuesta = mvc.perform(get("/api/v1/explorador/ventas")
                .param("pagina", "0").param("tamano", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[0].id").value(caso.venta().getId().intValue()))
            .andExpect(jsonPath("$.contenido[0].estado").value("PAGADA"))
            .andExpect(jsonPath("$.contenido[0].clienteId").value(caso.cliente().getId().intValue()))
            .andExpect(jsonPath("$.contenido[0].cantidadEntradas").value(1))
            .andExpect(jsonPath("$.contenido[0].total").value(12000.0))
            .andReturn().getResponse().getContentAsString();
        assertFalse(respuesta.contains("codigoQR"));
        assertFalse(respuesta.contains("numeroTarjeta"));
        assertEquals(EstadoVenta.PAGADA, ventas.findById(caso.venta().getId()).orElseThrow().getEstado());
    }

    @Test
    void valoresDePaginacionInvalidosDevuelven400() throws Exception {
        mvc.perform(get("/api/v1/explorador/entradas").param("pagina", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDACION"));
        mvc.perform(get("/api/v1/explorador/ventas").param("tamano", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/explorador/ventas").param("tamano", "51"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/explorador/entradas").param("eventoId", "-1"))
            .andExpect(status().isBadRequest());
    }
}
