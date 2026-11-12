package com.smartticket.service;

import com.smartticket.exception.SolicitudInvalidaException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Simulacion determinista: sin red, sin tarjetas reales, sin cargos reales. */
@Component
public class PasarelaPagoSimulada implements PasarelaPago {
    @Override
    public boolean cobrar(String tarjetaDePrueba, BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new SolicitudInvalidaException("El monto debe ser mayor a cero");
        }
        return switch (tarjetaDePrueba) {
            case "APROBADA" -> true;
            case "RECHAZADA" -> false;
            default -> throw new SolicitudInvalidaException(
                "Tarjeta de prueba invalida: use APROBADA o RECHAZADA");
        };
    }
}
