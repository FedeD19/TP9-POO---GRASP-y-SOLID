package com.smartticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartticket.model.EstadoEntrada;
import com.smartticket.model.Entrada;
import com.smartticket.repository.EntradaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CotizacionTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntradaRepository entradas;

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private MvcResult postCreacion(String ruta, Object cuerpo) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON)
                .content(json(cuerpo))).andExpect(status().isCreated()).andReturn();
    }

    private long id(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long lugar() throws Exception {
        return id(postCreacion("/api/v1/lugares",
            Map.of("nombre", "Estadio Central", "direccion", "Av. Principal 123")));
    }

    private long sector(long lugarId, String nombre, int capacidad) throws Exception {
        return id(postCreacion("/api/v1/sectores",
            Map.of("nombre", nombre, "capacidadMaxima", capacidad, "lugarId", lugarId)));
    }

    private long evento(long lugarId, long sectorId, String precio) throws Exception {
        return id(postCreacion("/api/v1/eventos", Map.of(
            "nombre", "Rock Fest", "fechaHora", "2026-12-20T21:00:00", "lugarId", lugarId,
            "preciosSectores", List.of(Map.of("sectorId", sectorId, "precio", precio)))));
    }

    private org.springframework.test.web.servlet.ResultActions cotizar(long eventoId, long sectorId, int cantidad)
            throws Exception {
        return mvc.perform(post("/api/v1/cotizaciones/cotizar")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("eventoId", eventoId, "sectorId", sectorId, "cantidad", cantidad))));
    }

    @Test
    void hu1Escenario1DosEntradasDe12000Dan24000YNoModificanInventario() throws Exception {
        long lugarId = lugar();
        long sectorId = sector(lugarId, "Platea", 3);
        long eventoId = evento(lugarId, sectorId, "12000.00");

        cotizar(eventoId, sectorId, 2)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventoId").value(Math.toIntExact(eventoId)))
            .andExpect(jsonPath("$.sectorId").value(Math.toIntExact(sectorId)))
            .andExpect(jsonPath("$.cantidad").value(2))
            .andExpect(jsonPath("$.precioUnitario").value(12000.00))
            .andExpect(jsonPath("$.subtotal").value(24000.00))
            .andExpect(jsonPath("$.total").value(24000.00))
            .andExpect(jsonPath("$.entradasDisponibles").value(3));

        assertEquals(3, entradas.countByEvento_IdAndSector_IdAndEstado(
            eventoId, sectorId, EstadoEntrada.DISPONIBLE));
        assertTrue(entradas.findAll().stream().allMatch(e -> e.getVenta() == null));
        // Cotizar dos veces no disminuye el stock: aun no hay reserva ni pago.
        cotizar(eventoId, sectorId, 2).andExpect(status().isOk())
            .andExpect(jsonPath("$.entradasDisponibles").value(3));
    }

    @Test
    void hu1Escenario2FaltaDisponibilidadDevuelve400() throws Exception {
        long lugarId = lugar();
        long vip = sector(lugarId, "VIP", 1);
        long eventoId = evento(lugarId, vip, "25000.00");

        cotizar(eventoId, vip, 3)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDACION"))
            .andExpect(jsonPath("$.detalle").value("Capacidad insuficiente o entradas no disponibles"));
    }

    @Test
    void hu1Escenario3CantidadCeroONegativaDevuelve400() throws Exception {
        long lugarId = lugar();
        long platea = sector(lugarId, "Platea", 2);
        long eventoId = evento(lugarId, platea, "12000.00");

        for (int cantidad : new int[] {0, -1, -10}) {
            cotizar(eventoId, platea, cantidad)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle")
                    .value("La cantidad de entradas solicitadas debe ser mayor a cero"));
        }
        assertEquals(2, entradas.countByEvento_IdAndSector_IdAndEstado(
            eventoId, platea, EstadoEntrada.DISPONIBLE));
    }

    @Test
    void noCuentaEntradasReservadasNiEmitidasComoDisponibles() throws Exception {
        long lugarId = lugar();
        long platea = sector(lugarId, "Platea", 3);
        long eventoId = evento(lugarId, platea, "12000.00");
        List<Entrada> creadas = entradas.findAll();
        creadas.get(0).setEstado(EstadoEntrada.RESERVADA);
        creadas.get(1).setEstado(EstadoEntrada.EMITIDA);
        entradas.saveAllAndFlush(creadas);

        cotizar(eventoId, platea, 2)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDACION"));
        cotizar(eventoId, platea, 1)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entradasDisponibles").value(1));
    }

    @Test
    void eventoOSectorIncorrectosDan404YDatosFaltantes400() throws Exception {
        long lugarId = lugar();
        long platea = sector(lugarId, "Platea", 2);
        long vip = sector(lugarId, "VIP", 1);
        // Los precios del evento se deben indicar para TODOS los sectores del lugar.
        long eventoId = id(postCreacion("/api/v1/eventos", Map.of(
            "nombre", "Rock Fest", "fechaHora", "2026-12-20T21:00:00", "lugarId", lugarId,
            "preciosSectores", List.of(Map.of("sectorId", platea, "precio", 12000),
                                         Map.of("sectorId", vip, "precio", 25000)))));
        long otroLugar = lugar();
        long externo = sector(otroLugar, "Campo", 2);

        cotizar(999999L, platea, 1).andExpect(status().isNotFound());
        cotizar(eventoId, externo, 1).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/cotizaciones/cotizar")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("eventoId", eventoId, "sectorId", platea))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDACION"));
    }

    @Test
    void usaElPrecioDelEventoConsultadoNoUnPrecioGlobalDelSector() throws Exception {
        long lugarId = lugar();
        long platea = sector(lugarId, "Platea", 2);
        long primerEvento = evento(lugarId, platea, "12000.00");
        long segundoEvento = evento(lugarId, platea, "18000.00");

        cotizar(primerEvento, platea, 2)
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(24000.00));
        cotizar(segundoEvento, platea, 2)
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(36000.00));
    }
}
