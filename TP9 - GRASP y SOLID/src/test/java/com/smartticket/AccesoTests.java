package com.smartticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartticket.model.Entrada;
import com.smartticket.model.EstadoEntrada;
import com.smartticket.model.EstadoVenta;
import com.smartticket.model.Venta;
import com.smartticket.repository.ClienteRepository;
import com.smartticket.repository.EntradaRepository;
import com.smartticket.repository.VentaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HU 3: cuatro escenarios del enunciado y proteccion del estado/hora de ingreso. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccesoTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private Clock reloj;
    @Autowired private EntradaRepository entradas;
    @Autowired private VentaRepository ventas;
    @Autowired private ClienteRepository clientes;

    record Escenario(long eventoId, long sectorId, long clienteId) {}

    private String json(Object objeto) throws Exception {
        return mapper.writeValueAsString(objeto);
    }

    private long crear(String ruta, Object cuerpo) throws Exception {
        MvcResult r = mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON)
                .content(json(cuerpo)))
            .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private Escenario preparar(int capacidad, int diasDesdeHoy) throws Exception {
        long lugar = crear("/api/v1/lugares", Map.of(
            "nombre", "Estadio para acceso", "direccion", "Calle 123"));
        long sector = crear("/api/v1/sectores", Map.of(
            "nombre", "Platea", "capacidadMaxima", capacidad, "lugarId", lugar));
        LocalDate fecha = LocalDate.now(reloj).plusDays(diasDesdeHoy);
        long evento = crear("/api/v1/eventos", Map.of(
            "nombre", "Festival de prueba", "fechaHora", fecha.atTime(21, 0).toString(),
            "lugarId", lugar,
            "preciosSectores", List.of(Map.of("sectorId", sector, "precio", 12000))));
        long cliente = crear("/api/v1/clientes", Map.of(
            "nombre", "Cliente de acceso", "email", "cliente-acceso-" + evento + "@example.com"));
        return new Escenario(evento, sector, cliente);
    }

    private JsonNode pagar(Escenario escenario) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/pagos/iniciar-pago")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("clienteId", escenario.clienteId(),
                    "eventoId", escenario.eventoId(), "sectorId", escenario.sectorId(),
                    "cantidad", 1, "tarjetaPrueba", "APROBADA"))))
            .andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString());
    }

    private MvcResult escanear(String codigo, int respuestaHttp) throws Exception {
        return mvc.perform(post("/api/v1/accesos/escanear-acceso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("codigoQR", codigo))))
            .andExpect(status().is(respuestaHttp)).andReturn();
    }

    private Entrada entradaDe(Escenario escenario) {
        return entradas.findByEvento_IdAndSector_IdAndEstadoOrderByIdAsc(
            escenario.eventoId(), escenario.sectorId(), EstadoEntrada.DISPONIBLE,
            PageRequest.of(0, 1)).get(0);
    }

    @Test
    void hu3IngresoPermitidoRegistraHoraYMarcaEntradaUtilizada() throws Exception {
        Escenario caso = preparar(2, 0);
        JsonNode comprobante = pagar(caso);
        JsonNode ticket = comprobante.get("entradas").get(0);
        String qr = ticket.get("codigoQR").asText();
        long entradaId = ticket.get("entradaId").asLong();

        MvcResult r = escanear(qr, 200);
        JsonNode respuesta = mapper.readTree(r.getResponse().getContentAsString());
        assertEquals("Acceso Permitido", respuesta.get("mensaje").asText());
        assertEquals("UTILIZADA", respuesta.get("estado").asText());
        assertEquals(entradaId, respuesta.get("entradaId").asLong());
        assertEquals(caso.eventoId(), respuesta.get("eventoId").asLong());
        assertFalse(respuesta.get("fechaHoraIngreso").isNull());
        assertNotNull(entradas.findById(entradaId).orElseThrow().getFechaHoraIngreso());
        assertEquals(1, entradas.countByEvento_IdAndSector_IdAndEstado(
            caso.eventoId(), caso.sectorId(), EstadoEntrada.UTILIZADA));
    }

    @Test
    void hu3SegundoEscaneoRechazadoYNoSobrescribeHoraOriginal() throws Exception {
        Escenario caso = preparar(1, 0);
        JsonNode ticket = pagar(caso).get("entradas").get(0);
        long id = ticket.get("entradaId").asLong();
        String qr = ticket.get("codigoQR").asText();
        escanear(qr, 200);
        LocalDateTime hora = entradas.findById(id).orElseThrow().getFechaHoraIngreso();

        JsonNode error = mapper.readTree(escanear(qr, 409).getResponse().getContentAsString());
        assertEquals("CONFLICTO", error.get("error").asText());
        assertEquals("Entrada ya utilizada", error.get("detalle").asText());
        assertEquals(hora, entradas.findById(id).orElseThrow().getFechaHoraIngreso());
    }

    @Test
    void hu3QrFalsoDevuelve404() throws Exception {
        JsonNode error = mapper.readTree(escanear("qr-que-no-existe", 404)
            .getResponse().getContentAsString());
        assertEquals("NO_ENCONTRADO", error.get("error").asText());
        assertEquals("Ticket Inválido o Inexistente", error.get("detalle").asText());
    }

    @Test
    void hu3EntradaDisponibleNoPuedeIngresar() throws Exception {
        Escenario caso = preparar(1, 0);
        Entrada entrada = entradaDe(caso);
        JsonNode error = mapper.readTree(escanear(entrada.getCodigoQR(), 403)
            .getResponse().getContentAsString());
        assertEquals("ACCESO_DENEGADO", error.get("error").asText());
        assertEquals("Ticket no emitido / Falta de pago", error.get("detalle").asText());
        assertEquals(EstadoEntrada.DISPONIBLE, entrada.getEstado());
        assertNull(entrada.getFechaHoraIngreso());
    }

    @Test
    void hu3EntradaReservadaNoPuedeIngresar() throws Exception {
        Escenario caso = preparar(1, 0);
        Entrada entrada = entradaDe(caso);
        entrada.setEstado(EstadoEntrada.RESERVADA);
        entradas.saveAndFlush(entrada);
        JsonNode error = mapper.readTree(escanear(entrada.getCodigoQR(), 403)
            .getResponse().getContentAsString());
        assertEquals("ACCESO_DENEGADO", error.get("error").asText());
        assertEquals(EstadoEntrada.RESERVADA, entrada.getEstado());
        assertNull(entrada.getFechaHoraIngreso());
    }

    @Test
    void hu3EmitidaSinVentaNoPuedeIngresar() throws Exception {
        Escenario caso = preparar(1, 0);
        Entrada entrada = entradaDe(caso);
        entrada.setEstado(EstadoEntrada.EMITIDA);
        entradas.saveAndFlush(entrada);
        JsonNode error = mapper.readTree(escanear(entrada.getCodigoQR(), 403)
            .getResponse().getContentAsString());
        assertEquals("Ticket no emitido / Falta de pago", error.get("detalle").asText());
        assertEquals(EstadoEntrada.EMITIDA, entrada.getEstado());
        assertNull(entrada.getFechaHoraIngreso());
    }

    @Test
    void hu3EmitidaConVentaPendienteNoPuedeIngresar() throws Exception {
        Escenario caso = preparar(1, 0);
        Entrada entrada = entradaDe(caso);
        Venta pendiente = new Venta();
        pendiente.setCliente(clientes.findById(caso.clienteId()).orElseThrow());
        pendiente.setEstado(EstadoVenta.PENDIENTE);
        pendiente.setTotal(new java.math.BigDecimal("12000.00"));
        pendiente = ventas.saveAndFlush(pendiente);
        entrada.setVenta(pendiente);
        entrada.setEstado(EstadoEntrada.EMITIDA);
        entradas.saveAndFlush(entrada);
        JsonNode error = mapper.readTree(escanear(entrada.getCodigoQR(), 403)
            .getResponse().getContentAsString());
        assertEquals("Ticket no emitido / Falta de pago", error.get("detalle").asText());
        assertEquals(EstadoEntrada.EMITIDA, entrada.getEstado());
        assertNull(entrada.getFechaHoraIngreso());
    }

    @Test
    void hu3EventoDeOtroDiaNoPuedeIngresar() throws Exception {
        Escenario caso = preparar(1, 1);
        JsonNode ticket = pagar(caso).get("entradas").get(0);
        JsonNode error = mapper.readTree(escanear(ticket.get("codigoQR").asText(), 409)
            .getResponse().getContentAsString());
        assertEquals("La entrada no corresponde a un evento de hoy", error.get("detalle").asText());
        Entrada e = entradas.findById(ticket.get("entradaId").asLong()).orElseThrow();
        assertEquals(EstadoEntrada.EMITIDA, e.getEstado());
        assertNull(e.getFechaHoraIngreso());
    }

    @Test
    void hu3QrVacioOJsonFaltanteDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/accesos/escanear-acceso").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigoQR\":\"  \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDACION"));
        mvc.perform(post("/api/v1/accesos/escanear-acceso").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDACION"));
    }
}
