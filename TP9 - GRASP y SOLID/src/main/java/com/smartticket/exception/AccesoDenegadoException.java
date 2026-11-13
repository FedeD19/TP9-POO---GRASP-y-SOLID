package com.smartticket.exception;

/** La entrada existe, pero no esta habilitada para ingresar al evento. */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
