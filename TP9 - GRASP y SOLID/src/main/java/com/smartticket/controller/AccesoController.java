package com.smartticket.controller;

import com.smartticket.dto.AccesoRequest;
import com.smartticket.dto.AccesoResponse;
import com.smartticket.dto.ErrorResponse;
import com.smartticket.service.AccesoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accesos")
@Tag(name = "Accesos", description = "HU 3 - Control de acceso en puerta por QR")
public class AccesoController {
    private final AccesoService servicio;

    public AccesoController(AccesoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/escanear-acceso")
    @Operation(summary = "Validar QR e ingresar al evento",
        description = "Una entrada EMITIDA, con venta PAGADA y evento del dia puede "
            + "ingresar una sola vez. El codigo QR es el valor entregado por HU 2.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Acceso Permitido"),
        @ApiResponse(responseCode = "400", description = "JSON o QR obligatorio invalido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Ticket no emitido / Falta de pago",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket Inválido o Inexistente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Entrada ya utilizada o evento de otra fecha",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AccesoResponse> escanearAcceso(@Valid @RequestBody AccesoRequest datos) {
        return ResponseEntity.ok(servicio.escanear(datos));
    }
}
