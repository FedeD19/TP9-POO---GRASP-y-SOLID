package com.smartticket.exception;

import com.smartticket.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponse> solicitudInvalida(SolicitudInvalidaException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDACION", ex.getMessage()));
    }

    @ExceptionHandler(PagoDenegadoException.class)
    public ResponseEntity<ErrorResponse> pagoDenegado(PagoDenegadoException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ErrorResponse("PAGO_DENEGADO", ex.getMessage()));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICTO", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        var campo = ex.getBindingResult().getFieldErrors().stream().findFirst();
        String detalle = campo.map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Los datos enviados no son validos");
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDACION", detalle));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("FORMATO_INVALIDO",
                "Revisa el JSON y el formato de fecha ISO 8601 (ej. 2026-12-20T21:00:00)"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICTO",
                        "La operacion viola una restriccion de la base de datos; puede haber registros relacionados o datos duplicados"));
    }
}
