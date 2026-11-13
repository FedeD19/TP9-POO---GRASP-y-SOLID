package com.smartticket.service;

import com.smartticket.dto.EntradaResumenResponse;
import com.smartticket.dto.PaginaResponse;
import com.smartticket.dto.VentaResumenResponse;
import com.smartticket.exception.SolicitudInvalidaException;
import com.smartticket.model.Entrada;
import com.smartticket.model.Venta;
import com.smartticket.repository.EntradaRepository;
import com.smartticket.repository.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Etapa 4.5: consultas de solo lectura para el frontend, con paginas acotadas. */
@Service
public class ExploradorService {
    private static final int MAX_TAMANO = 50;
    private final EntradaRepository entradas;
    private final VentaRepository ventas;

    public ExploradorService(EntradaRepository entradas, VentaRepository ventas) {
        this.entradas = entradas;
        this.ventas = ventas;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<EntradaResumenResponse> listarEntradas(
            int pagina, int tamano, Long eventoId) {
        Pageable pageable = validarPagina(pagina, tamano);
        if (eventoId != null && eventoId <= 0) {
            throw new SolicitudInvalidaException("El ID de evento debe ser positivo");
        }
        Page<Entrada> resultado = eventoId == null
            ? entradas.findAll(pageable)
            : entradas.findByEvento_Id(eventoId, pageable);
        return PaginaResponse.desde(resultado.map(e -> new EntradaResumenResponse(
            e.getId(), e.getEvento().getId(), e.getEvento().getNombre(),
            e.getSector().getId(), e.getSector().getNombre(),
            e.getVenta() == null ? null : e.getVenta().getId(),
            e.getEstado(), e.getFechaHoraIngreso()
        )));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<VentaResumenResponse> listarVentas(int pagina, int tamano) {
        Page<Venta> resultado = ventas.findAll(validarPagina(pagina, tamano));
        return PaginaResponse.desde(resultado.map(v -> new VentaResumenResponse(
            v.getId(), v.getCliente().getId(), v.getCliente().getNombre(),
            v.getEstado(), v.getTotal(), entradas.countByVenta_Id(v.getId())
        )));
    }

    private Pageable validarPagina(int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > MAX_TAMANO) {
            throw new SolicitudInvalidaException(
                "La pagina debe ser >= 0 y el tamano debe estar entre 1 y " + MAX_TAMANO);
        }
        return PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "id"));
    }
}
