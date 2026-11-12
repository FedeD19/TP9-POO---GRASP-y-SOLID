package com.smartticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** La tarjeta es solo un TOKEN FICTICIO; no se aceptan datos bancarios reales. */
public record PagoRequest(
    @NotNull @Positive @Schema(example = "1") Long clienteId,
    @NotNull @Positive @Schema(example = "1") Long eventoId,
    @NotNull @Positive @Schema(example = "1") Long sectorId,
    @NotNull @Positive @Schema(example = "2") Integer cantidad,
    @NotBlank @Schema(description = "Solo para pruebas: APROBADA o RECHAZADA. Nunca use datos de tarjeta reales.",
                      allowableValues = {"APROBADA", "RECHAZADA"}, example = "APROBADA")
    String tarjetaPrueba
) {}
