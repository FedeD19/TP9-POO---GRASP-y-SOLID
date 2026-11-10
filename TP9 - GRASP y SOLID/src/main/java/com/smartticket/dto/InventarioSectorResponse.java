package com.smartticket.dto;

import java.math.BigDecimal;

public record InventarioSectorResponse(
    Long sectorId, String nombre, int capacidadMaxima, BigDecimal precio,
    long totalEntradas, long disponibles, long reservadas, long emitidas, long utilizadas
) {}
