package com.smartticket.dto;

import com.smartticket.model.EstadoEntrada;
import java.time.LocalDateTime;

/** Comprobante de ingreso: no incluye detalles de pagos ni datos personales. */
public record AccesoResponse(
    String mensaje,
    Long entradaId,
    Long eventoId,
    String evento,
    Long sectorId,
    EstadoEntrada estado,
    LocalDateTime fechaHoraIngreso
) {}
