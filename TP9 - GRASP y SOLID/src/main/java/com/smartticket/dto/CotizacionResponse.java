package com.smartticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Resultado informativo: cotizar NO reserva ni emite entradas. */
public record CotizacionResponse(
    Long eventoId,
    String evento,
    Long sectorId,
    String sector,
    Integer cantidad,
    @Schema(example = "12000.00") BigDecimal precioUnitario,
    @Schema(example = "24000.00") BigDecimal subtotal,
    @Schema(example = "24000.00") BigDecimal total,
    Long entradasDisponibles
) {}
