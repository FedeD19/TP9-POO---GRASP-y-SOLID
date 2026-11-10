package com.smartticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Cada test revierte sus datos, evitando dependencia entre pruebas.
class DominioCrudTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    private String json(Map<String, Object> datos) throws Exception {
        return mapper.writeValueAsString(datos);
    }

    private MvcResult crear(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn();
    }

    private long id(MvcResult resultado) throws Exception {
        JsonNode respuesta = mapper.readTree(resultado.getResponse().getContentAsString());
        return respuesta.get("id").asLong();
    }

    private long crearLugar() throws Exception {
        return id(crear("/api/v1/lugares",
                json(Map.of("nombre", "Estadio Central", "direccion", "Av. Principal 123"))));
    }

    @Test
    void lugarSePuedeCrearConsultarActualizarYEliminar() throws Exception {
        long lugarId = crearLugar();
        mvc.perform(get("/api/v1/lugares/{id}", lugarId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Estadio Central"));
        mvc.perform(put("/api/v1/lugares/{id}", lugarId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "Teatro Norte", "direccion", "Calle 10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Teatro Norte"));
        mvc.perform(get("/api/v1/lugares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) lugarId));
        mvc.perform(delete("/api/v1/lugares/{id}", lugarId))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/lugares/{id}", lugarId))
                .andExpect(status().isNotFound());
    }

    @Test
    void sectorSoloSeCreaConLugarExistenteYCapacidadPositiva() throws Exception {
        long lugarId = crearLugar();
        long sectorId = id(crear("/api/v1/sectores",
                json(Map.of("nombre", "Platea", "capacidadMaxima", 300, "lugarId", lugarId))));
        mvc.perform(get("/api/v1/sectores/{id}", sectorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lugarId").value((int) lugarId));
        mvc.perform(put("/api/v1/sectores/{id}", sectorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "VIP", "capacidadMaxima", 100, "lugarId", lugarId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidadMaxima").value(100));
        mvc.perform(post("/api/v1/sectores").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "VIP", "capacidadMaxima", 0, "lugarId", lugarId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDACION"));
        mvc.perform(post("/api/v1/sectores").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "VIP", "capacidadMaxima", 50, "lugarId", 999999))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/sectores/{id}", sectorId))
                .andExpect(status().isNoContent());
    }

    @Test
    void eventoSeAsociaConLugarYRequiereFecha() throws Exception {
        long lugarId = crearLugar();
        long sectorId = id(crear("/api/v1/sectores",
                json(Map.of("nombre", "Platea", "capacidadMaxima", 3, "lugarId", lugarId))));
        long eventoId = id(crear("/api/v1/eventos",
                json(Map.of("nombre", "Rock Fest", "fechaHora", "2026-12-20T21:00:00", "lugarId", lugarId,
                        "preciosSectores", java.util.List.of(Map.of("sectorId", sectorId, "precio", 12000))))));
        mvc.perform(get("/api/v1/eventos/{id}", eventoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Rock Fest"))
                .andExpect(jsonPath("$.lugarId").value((int) lugarId));
        mvc.perform(post("/api/v1/eventos").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "Sin fecha", "lugarId", lugarId))))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/eventos/{id}", eventoId))
                .andExpect(status().isNoContent());
    }

    @Test
    void clienteValidaEmailUnicoYPermiteActualizar() throws Exception {
        long clienteId = id(crear("/api/v1/clientes",
                json(Map.of("nombre", "Ana Perez", "email", "Ana@Example.com"))));
        mvc.perform(get("/api/v1/clientes/{id}", clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "Otra", "email", "ana@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTO"));
        mvc.perform(put("/api/v1/clientes/{id}", clienteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "Ana Nueva", "email", "nueva@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Nueva"));
        mvc.perform(delete("/api/v1/clientes/{id}", clienteId))
                .andExpect(status().isNoContent());
    }

    @Test
    void noSePuedeEliminarLugarConSectoresAsociados() throws Exception {
        long lugarId = crearLugar();
        crear("/api/v1/sectores",
                json(Map.of("nombre", "Platea", "capacidadMaxima", 300, "lugarId", lugarId)));
        mvc.perform(delete("/api/v1/lugares/{id}", lugarId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTO"));
    }

    @Test
    void noSePuedeEliminarLugarConEventosAsociados() throws Exception {
        long lugarId = crearLugar();
        long sectorId = id(crear("/api/v1/sectores",
                json(Map.of("nombre", "VIP", "capacidadMaxima", 2, "lugarId", lugarId))));
        crear("/api/v1/eventos",
                json(Map.of("nombre", "Rock Fest", "fechaHora", "2026-12-20T21:00:00", "lugarId", lugarId,
                        "preciosSectores", java.util.List.of(Map.of("sectorId", sectorId, "precio", 25000)))));
        mvc.perform(delete("/api/v1/lugares/{id}", lugarId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTO"));
    }

}
