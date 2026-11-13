package com.smartticket.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Contrato REST de paginacion estable, independiente de la serializacion de Spring Page. */
public record PaginaResponse<T>(
    List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas
) {
    public static <T> PaginaResponse<T> desde(Page<T> pagina) {
        return new PaginaResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
            pagina.getTotalElements(), pagina.getTotalPages());
    }
}
