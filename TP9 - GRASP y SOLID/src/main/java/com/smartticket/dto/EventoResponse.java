package com.smartticket.dto;

import java.time.LocalDateTime;

public record EventoResponse(Long id, String nombre, LocalDateTime fechaHora, Long lugarId) {}
