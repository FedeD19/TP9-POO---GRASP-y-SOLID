package com.smartticket.controller;

import com.smartticket.dto.EventoRequest;
import com.smartticket.dto.EventoResponse;
import com.smartticket.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/eventos")
@Tag(name = "Eventos", description = "Etapa 2 - Administracion de eventos")
public class EventoController {
    private final EventoService service;

    public EventoController(EventoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear evento")
    public ResponseEntity<EventoResponse> crear(@Valid @RequestBody EventoRequest datos) {
        EventoResponse nuevo = service.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/eventos/" + nuevo.id())).body(nuevo);
    }

    @GetMapping
    @Operation(summary = "Listar eventos")
    public List<EventoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar evento por ID")
    public EventoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar evento por ID")
    public EventoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EventoRequest datos) {
        return service.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar evento por ID")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
