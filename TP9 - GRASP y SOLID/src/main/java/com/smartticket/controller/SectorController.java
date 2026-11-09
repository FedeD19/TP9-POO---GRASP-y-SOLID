package com.smartticket.controller;

import com.smartticket.dto.SectorRequest;
import com.smartticket.dto.SectorResponse;
import com.smartticket.service.SectorService;
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
@RequestMapping("/api/v1/sectores")
@Tag(name = "Sectores", description = "Etapa 2 - Administracion de sectores")
public class SectorController {
    private final SectorService service;

    public SectorController(SectorService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear sector")
    public ResponseEntity<SectorResponse> crear(@Valid @RequestBody SectorRequest datos) {
        SectorResponse nuevo = service.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/sectores/" + nuevo.id())).body(nuevo);
    }

    @GetMapping
    @Operation(summary = "Listar sectores")
    public List<SectorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar sector por ID")
    public SectorResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar sector por ID")
    public SectorResponse actualizar(@PathVariable Long id, @Valid @RequestBody SectorRequest datos) {
        return service.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar sector por ID")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
