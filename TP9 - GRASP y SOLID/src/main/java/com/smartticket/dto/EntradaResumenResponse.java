package com.smartticket.dto;

import com.smartticket.model.EstadoEntrada;
import java.time.LocalDateTime;

/** No incluye el codigoQR: es una credencial de ingreso, no un dato publico del explorador. */
public record EntradaResumenResponse(
    Long id, Long eventoId, String evento, Long sectorId, String sector,
    Long ventaId, EstadoEntrada estado, LocalDateTime fechaHoraIngreso
) {}
