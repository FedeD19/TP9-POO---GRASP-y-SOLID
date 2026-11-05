package com.smartticket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cotizaciones")
@Tag(name = "Cotizaciones", description = "HU 1 - Seleccion y cotizacion")
public class CotizacionController {
    @PostMapping("/cotizar")
    @Operation(summary = "Cotizar entradas", description = "Pendiente de implementar en HU 1")
    public ResponseEntity<String> cotizar() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("Etapa 1: cotizacion aun no implementada");
    }
}
