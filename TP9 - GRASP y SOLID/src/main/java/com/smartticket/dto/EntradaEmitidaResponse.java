package com.smartticket.dto;

import com.smartticket.model.EstadoEntrada;

public record EntradaEmitidaResponse(Long entradaId, EstadoEntrada estado, String codigoQR) {}
