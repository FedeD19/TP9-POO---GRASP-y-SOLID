package com.smartticket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Infraestructura", description = "Verificacion tecnica del servidor")
public class EstadoController {
    @GetMapping("/estado")
    @Operation(summary = "Comprobar que la API esta activa")
    public Map<String, String> estado() {
        return Map.of("servicio", "SmartTicket", "estado", "OK");
    }
}
