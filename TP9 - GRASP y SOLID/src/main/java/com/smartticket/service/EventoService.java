package com.smartticket.service;

import com.smartticket.dto.EventoRequest;
import com.smartticket.dto.EventoResponse;
import com.smartticket.exception.RecursoNoEncontradoException;
import com.smartticket.model.Evento;
import com.smartticket.model.Lugar;
import com.smartticket.repository.EventoRepository;
import com.smartticket.repository.LugarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class EventoService {
    private final EventoRepository repository;
    private final LugarRepository lugares;

    public EventoService(EventoRepository repository, LugarRepository lugares) {
        this.repository = repository;
        this.lugares = lugares;
    }

    @Transactional
    public EventoResponse crear(EventoRequest datos) {
        Evento evento = new Evento();
        aplicarDatos(evento, datos);
        return convertir(repository.save(evento));
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listar() {
        return repository.findAll().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse buscar(Long id) {
        return convertir(obtenerEntidad(id));
    }

    @Transactional
    public EventoResponse actualizar(Long id, EventoRequest datos) {
        Evento evento = obtenerEntidad(id);
        aplicarDatos(evento, datos);
        return convertir(repository.save(evento));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(obtenerEntidad(id));
        repository.flush();
    }

    private void aplicarDatos(Evento evento, EventoRequest datos) {
        Lugar lugar = lugares.findById(datos.lugarId()).orElseThrow(() ->
                new RecursoNoEncontradoException("Lugar no encontrado: " + datos.lugarId()));
        evento.setNombre(datos.nombre().trim());
        evento.setFechaHora(datos.fechaHora());
        evento.setLugar(lugar);
    }

    private Evento obtenerEntidad(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Evento no encontrado: " + id));
    }

    private EventoResponse convertir(Evento evento) {
        return new EventoResponse(evento.getId(), evento.getNombre(),
                evento.getFechaHora(), evento.getLugar().getId());
    }
}
