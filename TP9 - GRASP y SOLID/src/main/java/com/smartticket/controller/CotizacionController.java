package com.smartticket.controller;

import com.smartticket.dto.CotizacionRequest;
import com.smartticket.dto.CotizacionResponse;
import com.smartticket.dto.ErrorResponse;
import com.smartticket.service.CotizacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cotizaciones")
@Tag(name = "Cotizaciones", description = "HU 1 - Seleccion y cotizacion")
public class CotizacionController {
    private final CotizacionService service;

    public CotizacionController(CotizacionService service) {
        this.service = service;
    }

    @PostMapping("/cotizar")
    @Operation(summary = "Cotizar entradas sin reservarlas",
        description = "Valida la cantidad y el inventario DISPONIBLE de un evento y sector. "
            + "Calcula subtotal y total con BigDecimal. No cambia el estado de las entradas.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cotizacion valida"),
        @ApiResponse(responseCode = "400", description = "Cantidad invalida o inventario insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Evento o sector del evento inexistente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CotizacionResponse> cotizar(@Valid @RequestBody CotizacionRequest datos) {
        return ResponseEntity.ok(service.cotizar(datos));
    }
}
