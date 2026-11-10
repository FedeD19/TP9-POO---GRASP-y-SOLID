package com.smartticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/** Alta de evento: exige fijar el precio de CADA sector existente del lugar. */
public record EventoCreacionRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    @Schema(example = "Rock Fest") String nombre,

    @NotNull(message = "La fecha y hora son obligatorias")
    @Schema(example = "2026-12-20T21:00:00") LocalDateTime fechaHora,

    @NotNull(message = "Debe indicar el ID del lugar")
    @Positive(message = "El ID del lugar debe ser positivo")
    @Schema(example = "1") Long lugarId,

    @NotEmpty(message = "Debe indicar un precio para cada sector")
    List<@NotNull @Valid PrecioSectorRequest> preciosSectores
) {}
