package com.smartticket.controller;

import com.smartticket.dto.ErrorResponse;
import com.smartticket.dto.PagoRequest;
import com.smartticket.dto.PagoResponse;
import com.smartticket.service.PagoService;
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
@RequestMapping("/api/v1/pagos")
@Tag(name = "Pagos", description = "HU 2 - Pasarela simulada y emision de entradas")
public class PagoController {
    private final PagoService service;

    public PagoController(PagoService service) { this.service = service; }

    @PostMapping("/iniciar-pago")
    @Operation(summary = "Pagar y emitir entradas",
        description = "Usar exclusivamente tarjetaPrueba APROBADA o RECHAZADA. "
            + "El pago es ficticio e inmediato; se vuelve a comprobar disponibilidad. "
            + "No enviar datos bancarios reales.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pago aprobado y entradas emitidas"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "402", description = "Pago denegado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Cliente, evento o sector inexistente"),
        @ApiResponse(responseCode = "409", description = "Inventario agotado o comprado por otro cliente")
    })
    public ResponseEntity<PagoResponse> iniciarPago(@Valid @RequestBody PagoRequest datos) {
        return ResponseEntity.ok(service.iniciarPago(datos));
    }
}
