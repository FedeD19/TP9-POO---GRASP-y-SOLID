package com.smartticket.service;

import com.smartticket.dto.CotizacionRequest;
import com.smartticket.dto.CotizacionResponse;
import com.smartticket.exception.RecursoNoEncontradoException;
import com.smartticket.exception.SolicitudInvalidaException;
import com.smartticket.model.EstadoEntrada;
import com.smartticket.model.Evento;
import com.smartticket.model.PrecioSector;
import com.smartticket.repository.EntradaRepository;
import com.smartticket.repository.EventoRepository;
import com.smartticket.repository.PrecioSectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** HU 1: verifica inventario y calcula un precio; no modifica ninguna entidad. */
@Service
public class CotizacionService {
    private final EventoRepository eventos;
    private final PrecioSectorRepository precios;
    private final EntradaRepository entradas;

    // Inyeccion por constructor: dependemos de abstracciones de persistencia.
    public CotizacionService(EventoRepository eventos,
                             PrecioSectorRepository precios,
                             EntradaRepository entradas) {
        this.eventos = eventos;
        this.precios = precios;
        this.entradas = entradas;
    }

    @Transactional(readOnly = true)
    public CotizacionResponse cotizar(CotizacionRequest datos) {
        // Regla de negocio que se comprueba antes de consultar la base de datos.
        if (datos.cantidad() == null || datos.cantidad() <= 0) {
            throw new SolicitudInvalidaException(
                "La cantidad de entradas solicitadas debe ser mayor a cero");
        }

        Evento evento = eventos.findById(datos.eventoId())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Evento no encontrado: " + datos.eventoId()));

        // El precio solo existe cuando el sector pertenece a este evento.
        PrecioSector precioSector = precios.findByEvento_IdAndSector_Id(
                evento.getId(), datos.sectorId())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Sector no disponible para el evento: " + datos.sectorId()));

        // Importa el estado DISPONIBLE, no la capacidad nominal del sector.
        long disponibles = entradas.countByEvento_IdAndSector_IdAndEstado(
                evento.getId(), datos.sectorId(), EstadoEntrada.DISPONIBLE);
        if (disponibles < datos.cantidad()) {
            throw new SolicitudInvalidaException(
                "Capacidad insuficiente o entradas no disponibles");
        }

        BigDecimal precioUnitario = precioSector.getPrecio();
        BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(datos.cantidad()));
        // La consigna no incluye impuestos, cargos ni descuentos.
        BigDecimal total = subtotal;

        return new CotizacionResponse(
            evento.getId(), evento.getNombre(),
            precioSector.getSector().getId(), precioSector.getSector().getNombre(),
            datos.cantidad(), precioUnitario, subtotal, total, disponibles);
    }
}
