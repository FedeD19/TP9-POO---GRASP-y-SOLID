package com.smartticket.controller;

import com.smartticket.dto.LugarRequest;
import com.smartticket.dto.LugarResponse;
import com.smartticket.service.LugarService;
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
@RequestMapping("/api/v1/lugares")
@Tag(name = "Lugares", description = "Etapa 2 - Administracion de lugares")
public class LugarController {
    private final LugarService service;

    public LugarController(LugarService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear lugar")
    public ResponseEntity<LugarResponse> crear(@Valid @RequestBody LugarRequest datos) {
        LugarResponse nuevo = service.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/lugares/" + nuevo.id())).body(nuevo);
    }

    @GetMapping
    @Operation(summary = "Listar lugares")
    public List<LugarResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar lugar por ID")
    public LugarResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar lugar por ID")
    public LugarResponse actualizar(@PathVariable Long id, @Valid @RequestBody LugarRequest datos) {
        return service.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar lugar por ID")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
