package com.smartticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Datos de una solicitud de cotizacion; no identifica ni bloquea entradas concretas. */
public record CotizacionRequest(
    @NotNull(message = "El evento es obligatorio")
    @Positive(message = "El ID del evento debe ser positivo")
    @Schema(description = "Identificador del evento", example = "1")
    Long eventoId,

    @NotNull(message = "El sector es obligatorio")
    @Positive(message = "El ID del sector debe ser positivo")
    @Schema(description = "Identificador del sector del evento", example = "1")
    Long sectorId,

    @NotNull(message = "La cantidad es obligatoria")
    @Schema(description = "Cantidad de entradas solicitadas; debe ser mayor a cero", example = "2", minimum = "1")
    Integer cantidad
) {}
