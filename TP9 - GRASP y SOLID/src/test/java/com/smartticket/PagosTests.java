package com.smartticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartticket.model.EstadoEntrada;
import com.smartticket.model.EstadoVenta;
import com.smartticket.repository.EntradaRepository;
import com.smartticket.repository.VentaRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HU2: flujo completo desde controladores hasta persistencia. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PagosTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntradaRepository entradas;
    @Autowired private VentaRepository ventas;

    private String json(Object o) throws Exception { return mapper.writeValueAsString(o); }

    private long crear(String ruta, Object cuerpo) throws Exception {
        MvcResult r = mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON)
            .content(json(cuerpo))).andExpect(status().isCreated()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private long lugar() throws Exception {
        return crear("/api/v1/lugares", Map.of("nombre", "Estadio Central", "direccion", "Av Principal 1"));
    }

    private long sector(long lugarId, int capacidad) throws Exception {
        return crear("/api/v1/sectores", Map.of(
            "nombre", "Platea", "lugarId", lugarId, "capacidadMaxima", capacidad));
    }

    private long evento(long lugarId, long sectorId) throws Exception {
        return crear("/api/v1/eventos", Map.of(
            "nombre", "Rock Fest", "fechaHora", "2026-12-20T21:00:00",
            "lugarId", lugarId,
            "preciosSectores", List.of(Map.of("sectorId", sectorId, "precio", 12000))));
    }

    private long cliente(String sufijo) throws Exception {
        return crear("/api/v1/clientes", Map.of("nombre", "Ana " + sufijo,
            "email", "ana" + sufijo + "@example.com"));
    }

    private MvcResult pagar(long cliente, long evento, long sector, int cantidad, String tarjeta, int estado)
            throws Exception {
        return mvc.perform(post("/api/v1/pagos/iniciar-pago")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("clienteId", cliente, "eventoId", evento, "sectorId", sector,
                "cantidad", cantidad, "tarjetaPrueba", tarjeta))))
            .andExpect(status().is(estado)).andReturn();
    }

    @Test
    void hu2PagoAprobadoCreaVentaPagadaEmiteDosEntradasYEntregaQr() throws Exception {
        long lugar = lugar(), sector = sector(lugar, 3), evento = evento(lugar, sector), cliente = cliente("a");
        MvcResult r = pagar(cliente, evento, sector, 2, "APROBADA", 200);
        JsonNode cuerpo = mapper.readTree(r.getResponse().getContentAsString());
        assertTrue(cuerpo.get("ventaId").asLong() > 0);
        assertEquals("PAGADA", cuerpo.get("estado").asText());
        assertEquals(24000, cuerpo.get("total").asDouble(), 0.001);
        assertEquals(2, cuerpo.get("entradas").size());
        for (JsonNode ticket : cuerpo.get("entradas")) {
            assertEquals("EMITIDA", ticket.get("estado").asText());
            assertFalse(ticket.get("codigoQR").asText().isBlank());
        }
        assertEquals(2, entradas.countByEvento_IdAndSector_IdAndEstado(
            evento, sector, EstadoEntrada.EMITIDA));
        assertEquals(1, entradas.countByEvento_IdAndSector_IdAndEstado(
            evento, sector, EstadoEntrada.DISPONIBLE));
        var venta = ventas.findById(cuerpo.get("ventaId").asLong()).orElseThrow();
        assertEquals(EstadoVenta.PAGADA, venta.getEstado());
        assertEquals(0, venta.getTotal().compareTo(new java.math.BigDecimal("24000.00")));
    }

    @Test
    void hu2PagoRechazadoNoCreaVentaNiModificaEntradas() throws Exception {
        long lugar = lugar(), sector = sector(lugar, 2), evento = evento(lugar, sector), cliente = cliente("b");
        MvcResult r = pagar(cliente, evento, sector, 2, "RECHAZADA", 402);
        JsonNode error = mapper.readTree(r.getResponse().getContentAsString());
        assertEquals("PAGO_DENEGADO", error.get("error").asText());
        assertEquals("Pago denegado", error.get("detalle").asText());
        assertEquals(0, ventas.count());
        assertEquals(2, entradas.countByEvento_IdAndSector_IdAndEstado(
            evento, sector, EstadoEntrada.DISPONIBLE));
        assertTrue(entradas.findAll().stream().noneMatch(e -> e.getVenta() != null));
    }

    @Test
    void hu2PerdidaDeDisponibilidadSeDetectaAntesDeIntentarCobrar() throws Exception {
        long lugar = lugar(), sector = sector(lugar, 2), evento = evento(lugar, sector);
        long comprador1 = cliente("c"), comprador2 = cliente("d");
        pagar(comprador1, evento, sector, 2, "APROBADA", 200);
        // Aun con token RECHAZADA, la respuesta debe ser de INVENTARIO (409), no de pasarela (402).
        MvcResult r = pagar(comprador2, evento, sector, 1, "RECHAZADA", 409);
        assertEquals("Los lugares seleccionados ya no se encuentran disponibles",
            mapper.readTree(r.getResponse().getContentAsString()).get("detalle").asText());
        assertEquals(1, ventas.count());
        assertEquals(2, entradas.countByEvento_IdAndSector_IdAndEstado(
            evento, sector, EstadoEntrada.EMITIDA));
    }

    @Test
    void cantidadCeroYNegativaDevuelven400() throws Exception {
        long lugar = lugar(), sector = sector(lugar, 2), evento = evento(lugar, sector), cliente = cliente("e");
        for (int cant : new int[] {0, -2}) {
            pagar(cliente, evento, sector, cant, "APROBADA", 400);
        }
        assertEquals(0, ventas.count());
    }

    @Test
    void clienteInexistenteYSectorDeOtroLugarDevuelven404() throws Exception {
        long lugar = lugar(), sector = sector(lugar, 1), evento = evento(lugar, sector), cliente = cliente("f");
        pagar(999999L, evento, sector, 1, "APROBADA", 404);
        long segundoLugar = lugar();
        long sectorExterno = sector(segundoLugar, 1);
        pagar(cliente, evento, sectorExterno, 1, "APROBADA", 404);
        assertEquals(0, ventas.count());
    }

    @Test
    void tokenDesconocidoNoCobraNiCreaVenta() throws Exception {
        long lugar = lugar(), sector = sector(lugar, 1), evento = evento(lugar, sector), cliente = cliente("g");
        pagar(cliente, evento, sector, 1, "DESCONOCIDA", 400);
        assertEquals(0, ventas.count());
        assertEquals(1, entradas.countByEvento_IdAndSector_IdAndEstado(
            evento, sector, EstadoEntrada.DISPONIBLE));
    }

    @Test
    void cotizacionPreviaNoReservaStockYDespuesDeComprarSeReduceInventario() throws Exception {
        long lugar = lugar(), sector = sector(lugar, 2), evento = evento(lugar, sector), cliente = cliente("h");
        mvc.perform(post("/api/v1/cotizaciones/cotizar")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("eventoId", evento, "sectorId", sector, "cantidad", 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(24000.0));
        pagar(cliente, evento, sector, 1, "APROBADA", 200);
        mvc.perform(get("/api/v1/eventos/{id}/inventario", evento))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sectores[0].disponibles").value(1));
    }
}
