package com.smartticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InfraestructuraTests {
    @Autowired
    private MockMvc mvc;

    @Test
    void elServidorResponde() throws Exception {
        mvc.perform(get("/api/v1/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"));
    }

    @Test
    void cotizacionSinCuerpoDevuelve400() throws Exception {
        // En HU 1 (Etapa 3.2), la cotizacion ya esta implementada.
        // Como el @RequestBody es obligatorio, un POST sin JSON debe devolver 400.
        mvc.perform(post("/api/v1/cotizaciones/cotizar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("FORMATO_INVALIDO"));
    }
}
