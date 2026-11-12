package com.smartticket.exception;

public class PagoDenegadoException extends RuntimeException {
    public PagoDenegadoException(String mensaje) { super(mensaje); }
}
