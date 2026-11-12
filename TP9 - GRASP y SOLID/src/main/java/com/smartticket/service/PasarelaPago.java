package com.smartticket.service;

import java.math.BigDecimal;

/** DIP: el negocio depende de un contrato, no de un proveedor de pagos. */
public interface PasarelaPago {
    boolean cobrar(String tarjetaDePrueba, BigDecimal monto);
}
