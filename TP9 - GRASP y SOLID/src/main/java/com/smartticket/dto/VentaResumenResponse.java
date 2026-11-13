package com.smartticket.dto;

import com.smartticket.model.EstadoVenta;
import java.math.BigDecimal;

/** Resumen de ventas sin datos de tarjetas ni codigos QR. */
public record VentaResumenResponse(
    Long id, Long clienteId, String cliente, EstadoVenta estado,
    BigDecimal total, long cantidadEntradas
) {}
