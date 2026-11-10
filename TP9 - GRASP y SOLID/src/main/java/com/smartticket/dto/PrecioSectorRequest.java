package com.smartticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PrecioSectorRequest(
    @NotNull(message = "Debe indicar el sector")
    @Positive(message = "El ID del sector debe ser positivo")
    @Schema(example = "1") Long sectorId,

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales")
    @Schema(example = "12000.00") BigDecimal precio
) {}
