package com.smartticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartticket.model.EstadoEntrada;
import com.smartticket.repository.EntradaRepository;
import com.smartticket.repository.PrecioSectorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PreciosYEntradasTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private PrecioSectorRepository precios;
    @Autowired private EntradaRepository entradas;

    private String json(Object obj) throws Exception { return mapper.writeValueAsString(obj); }
    private MvcResult crear(String url, Object data) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json(data)))
                .andExpect(status().isCreated()).andReturn();
    }
    private long id(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }
    private long lugar() throws Exception {
        return id(crear("/api/v1/lugares", Map.of("nombre","Estadio","direccion","Av. 123")));
    }
    private long sector(long lugarId, String nombre, int capacidad) throws Exception {
        return id(crear("/api/v1/sectores", Map.of("nombre",nombre,"capacidadMaxima",capacidad,"lugarId",lugarId)));
    }
    private Map<String,Object> precio(long sectorId, Object valor) {
        return Map.of("sectorId", sectorId, "precio", valor);
    }
    private Map<String,Object> evento(long lugarId, List<Map<String,Object>> importes) {
        return Map.of("nombre", "Rock Fest", "fechaHora", "2026-12-20T21:00:00",
                "lugarId", lugarId, "preciosSectores", importes);
    }

    @Test
    void generaEntradasSegunCapacidadYGuardaPreciosPorSector() throws Exception {
        long lugarId = lugar();
        long platea = sector(lugarId, "Platea", 3);
        long vip = sector(lugarId, "VIP", 2);
        long eventoId = id(crear("/api/v1/eventos", evento(lugarId,
                List.of(precio(platea, "12000.00"), precio(vip, "25000.00")))));

        mvc.perform(get("/api/v1/eventos/{id}/inventario", eventoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectores.length()").value(2))
                .andExpect(jsonPath("$.sectores[0].precio").value(12000.0))
                .andExpect(jsonPath("$.sectores[0].totalEntradas").value(3))
                .andExpect(jsonPath("$.sectores[0].disponibles").value(3))
                .andExpect(jsonPath("$.sectores[1].precio").value(25000.0))
                .andExpect(jsonPath("$.sectores[1].totalEntradas").value(2))
                .andExpect(jsonPath("$.sectores[1].disponibles").value(2))
                .andExpect(jsonPath("$.sectores[1].emitidas").value(0));
        assertEquals(5, entradas.count());
        assertEquals(2, precios.count());
        assertTrue(entradas.findAll().stream().allMatch(e -> e.getEstado() == EstadoEntrada.DISPONIBLE
                && e.getCodigoQR() != null && e.getVenta() == null));
        assertEquals(5, entradas.findAll().stream().map(e -> e.getCodigoQR()).distinct().count());
    }

    @Test
    void eventosDistintosPuedenTenerPreciosDistintosParaMismoSector() throws Exception {
        long lugarId = lugar();
        long sectorId = sector(lugarId, "Platea", 2);
        long primero = id(crear("/api/v1/eventos", evento(lugarId, List.of(precio(sectorId, 12000)))));
        long segundo = id(crear("/api/v1/eventos", evento(lugarId, List.of(precio(sectorId, 18000)))));
        mvc.perform(get("/api/v1/eventos/{id}/inventario", primero))
                .andExpect(jsonPath("$.sectores[0].precio").value(12000));
        mvc.perform(get("/api/v1/eventos/{id}/inventario", segundo))
                .andExpect(jsonPath("$.sectores[0].precio").value(18000));
        assertEquals(4, entradas.count());
    }

    @Test
    void faltanPreciosRepetidosYOtrosSectoresDanError400SinGuardarEvento() throws Exception {
        long lugarId = lugar();
        long platea = sector(lugarId, "Platea", 2);
        long vip = sector(lugarId, "VIP", 1);
        long otroLugar = lugar();
        long externo = sector(otroLugar, "Otro", 2);

        List<List<Map<String,Object>>> malos = List.of(
                List.of(precio(platea, 12000)),
                List.of(precio(platea, 12000), precio(platea, 15000)),
                List.of(precio(platea, 12000), precio(externo, 30000)),
                List.of(precio(platea, 0), precio(vip, 10000)),
                List.of(precio(platea, -1), precio(vip, 10000)));
        for (var lista : malos) {
            mvc.perform(post("/api/v1/eventos").contentType(MediaType.APPLICATION_JSON)
                    .content(json(evento(lugarId, lista))))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, entradas.count());
        assertEquals(0, precios.count());
        mvc.perform(get("/api/v1/eventos")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void noSePuedeCrearEventoSinSectoresNiSinPrecios() throws Exception {
        long lugarId = lugar();
        mvc.perform(post("/api/v1/eventos").contentType(MediaType.APPLICATION_JSON)
                .content(json(evento(lugarId, List.of(precio(1, 10000))))))
                .andExpect(status().isBadRequest());
        sector(lugarId, "Campo", 2);
        mvc.perform(post("/api/v1/eventos").contentType(MediaType.APPLICATION_JSON)
                .content(json(evento(lugarId, List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noSePuedeCambiarCapacidadLugarNiBorrarSectorConEntradas() throws Exception {
        long lugarId = lugar();
        long otroLugar = lugar();
        long sectorId = sector(lugarId, "VIP", 2);
        id(crear("/api/v1/eventos", evento(lugarId, List.of(precio(sectorId, 25000)))));
        mvc.perform(put("/api/v1/sectores/{id}", sectorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre","VIP","lugarId",lugarId,"capacidadMaxima",3))))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/sectores/{id}", sectorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre","VIP","lugarId",otroLugar,"capacidadMaxima",2))))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/sectores/{id}", sectorId)).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/sectores/{id}", sectorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre","VIP Preferencial","lugarId",lugarId,"capacidadMaxima",2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("VIP Preferencial"));
    }

    @Test
    void borrarEventoSinVentasEliminaSusEntradasYPrecios() throws Exception {
        long lugarId = lugar();
        long sectorId = sector(lugarId, "Platea", 3);
        long eventoId = id(crear("/api/v1/eventos", evento(lugarId, List.of(precio(sectorId, 12000)))));
        mvc.perform(delete("/api/v1/eventos/{id}", eventoId)).andExpect(status().isNoContent());
        assertEquals(0, entradas.count());
        assertEquals(0, precios.count());
        mvc.perform(get("/api/v1/eventos/{id}/inventario", eventoId)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/sectores/{id}", sectorId)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/lugares/{id}", lugarId)).andExpect(status().isNoContent());
    }

    @Test
    void noSePuedeCambiarLugarDelEventoPeroSiNombreYFecha() throws Exception {
        long lugarId = lugar();
        long otroLugar = lugar();
        long sectorId = sector(lugarId, "Campo", 2);
        long eventoId = id(crear("/api/v1/eventos", evento(lugarId, List.of(precio(sectorId, 1000)))));
        mvc.perform(put("/api/v1/eventos/{id}", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre","Nuevo nombre","fechaHora","2026-12-21T21:00:00","lugarId", otroLugar))))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/eventos/{id}", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre","Nuevo nombre","fechaHora","2026-12-21T21:00:00","lugarId", lugarId))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nuevo nombre"));
        assertEquals(2, entradas.count());
    }

    @Test
    void noSePuedeEliminarEventoConEntradasEmitidas() throws Exception {
        long lugarId = lugar();
        long sectorId = sector(lugarId, "VIP", 2);
        long eventoId = id(crear("/api/v1/eventos", evento(lugarId, List.of(precio(sectorId, 1000)))));
        var primera = entradas.findAll().get(0);
        primera.setEstado(EstadoEntrada.EMITIDA); // Simulacion del estado que introducira HU2.
        entradas.saveAndFlush(primera);
        mvc.perform(delete("/api/v1/eventos/{id}", eventoId)).andExpect(status().isConflict());
        assertEquals(2, entradas.count());
    }

}
