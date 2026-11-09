package com.smartticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LugarRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        @Schema(example = "Estadio Central") String nombre,

        @NotBlank(message = "La direccion es obligatoria")
        @Size(max = 200)
        @Schema(example = "Av. Principal 123") String direccion
) {}
