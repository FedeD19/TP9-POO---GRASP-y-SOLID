package com.smartticket.controller;

import com.smartticket.dto.EntradaResumenResponse;
import com.smartticket.dto.PaginaResponse;
import com.smartticket.dto.VentaResumenResponse;
import com.smartticket.service.ExploradorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dos consultas faltantes para el Explorador React: nunca cambian los estados del dominio. */
@RestController
@RequestMapping("/api/v1/explorador")
@Tag(name = "Explorador", description = "Etapa 4.5 - Consultas paginadas de solo lectura")
public class ExploradorController {
    private final ExploradorService service;

    public ExploradorController(ExploradorService service) {
        this.service = service;
    }

    @GetMapping("/entradas")
    @Operation(summary = "Listar entradas sin exponer los codigos QR (filtro opcional por evento)")
    public PaginaResponse<EntradaResumenResponse> entradas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            @RequestParam(required = false) Long eventoId) {
        return service.listarEntradas(pagina, tamano, eventoId);
    }

    @GetMapping("/ventas")
    @Operation(summary = "Listar ventas sin datos de tarjetas, con paginacion")
    public PaginaResponse<VentaResumenResponse> ventas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return service.listarVentas(pagina, tamano);
    }
}
