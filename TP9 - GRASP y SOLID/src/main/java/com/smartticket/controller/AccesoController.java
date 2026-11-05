package com.smartticket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accesos")
@Tag(name = "Accesos", description = "HU 3 - Validacion de entradas por QR")
public class AccesoController {
    @PostMapping("/escanear-acceso")
    @Operation(summary = "Escanear entrada", description = "Pendiente de implementar en HU 3")
    public ResponseEntity<String> escanearAcceso() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("Etapa 1: control de acceso aun no implementado");
    }
}
