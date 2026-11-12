package com.smartticket.dto;

import com.smartticket.model.EstadoVenta;
import java.math.BigDecimal;
import java.util.List;

/** Comprobante de pago. No contiene ningun dato de tarjeta. */
public record PagoResponse(
    Long ventaId,
    EstadoVenta estado,
    Long clienteId,
    Long eventoId,
    Long sectorId,
    int cantidad,
    BigDecimal total,
    List<EntradaEmitidaResponse> entradas
) {}
