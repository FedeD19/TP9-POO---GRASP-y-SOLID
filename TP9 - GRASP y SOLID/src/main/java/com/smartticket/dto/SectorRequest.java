package com.smartticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SectorRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        @Schema(example = "Platea") String nombre,

        @Positive(message = "La capacidad maxima debe ser mayor a cero")
        @Schema(example = "300") int capacidadMaxima,

        @NotNull(message = "Debe indicar el ID del lugar")
        @Positive(message = "El ID del lugar debe ser positivo")
        @Schema(example = "1") Long lugarId
) {}
