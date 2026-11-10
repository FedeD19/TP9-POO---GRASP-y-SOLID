package com.smartticket.dto;

import java.util.List;

public record InventarioEventoResponse(
    Long eventoId, String evento, List<InventarioSectorResponse> sectores
) {}
