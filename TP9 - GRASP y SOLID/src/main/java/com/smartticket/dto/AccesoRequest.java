package com.smartticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Codigo opaco generado al crear el evento y entregado cuando se paga. */
public record AccesoRequest(
    @NotBlank(message = "El codigo QR es obligatorio")
    @Schema(description = "Codigo QR de una entrada emitida", example = "d1b7a660-c14e-4d6d-80fb-e2b712d55c7e")
    String codigoQR
) {}
