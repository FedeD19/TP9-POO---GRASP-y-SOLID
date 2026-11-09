package com.smartticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record EventoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        @Schema(example = "Rock Fest") String nombre,

        @NotNull(message = "La fecha y hora son obligatorias")
        @Schema(example = "2026-12-20T21:00:00", description = "Fecha y hora ISO 8601, sin zona horaria")
        LocalDateTime fechaHora,

        @NotNull(message = "Debe indicar el ID del lugar")
        @Positive(message = "El ID del lugar debe ser positivo")
        @Schema(example = "1") Long lugarId
) {}
