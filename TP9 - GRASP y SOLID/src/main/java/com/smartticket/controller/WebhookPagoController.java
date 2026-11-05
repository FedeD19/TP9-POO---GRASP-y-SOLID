package com.smartticket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pagos")
@Tag(name = "Pagos", description = "HU 2 - Notificaciones de la pasarela")
public class WebhookPagoController {
    @PostMapping("/webhook-pagos")
    @Operation(summary = "Recibir webhook de pago", description = "Pendiente de implementar en HU 2")
    public ResponseEntity<String> recibirWebhook() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("Etapa 1: webhook de pago aun no implementado");
    }
}
