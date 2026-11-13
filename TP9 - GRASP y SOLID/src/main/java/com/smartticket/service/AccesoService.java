package com.smartticket.service;

import com.smartticket.dto.AccesoRequest;
import com.smartticket.dto.AccesoResponse;
import com.smartticket.exception.AccesoDenegadoException;
import com.smartticket.exception.ConflictoException;
import com.smartticket.exception.RecursoNoEncontradoException;
import com.smartticket.exception.SolicitudInvalidaException;
import com.smartticket.model.Entrada;
import com.smartticket.model.EstadoEntrada;
import com.smartticket.model.EstadoVenta;
import com.smartticket.repository.EntradaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU 3: valida el QR y registra un solo ingreso por entrada emitida y pagada. */
@Service
public class AccesoService {
    private final EntradaRepository entradas;
    private final Clock reloj;

    public AccesoService(EntradaRepository entradas, Clock reloj) {
        this.entradas = entradas;
        this.reloj = reloj;
    }

    @Transactional
    public AccesoResponse escanear(AccesoRequest solicitud) {
        // @Valid rechaza QR vacios en HTTP; tambien se valida en la capa de negocio.
        if (solicitud == null || solicitud.codigoQR() == null
                || solicitud.codigoQR().isBlank()) {
            throw new SolicitudInvalidaException("El codigo QR es obligatorio");
        }

        // Bloqueo exclusivo sobre la Entrada, ANTES de leer o cambiar el estado.
        // Asi dos escaneos simultaneos del mismo QR no pueden admitir ambos.
        Entrada entrada = entradas.bloquearPorCodigoQR(solicitud.codigoQR().trim())
            .orElseThrow(() -> new RecursoNoEncontradoException("Ticket Inválido o Inexistente"));

        if (entrada.getEstado() == EstadoEntrada.UTILIZADA) {
            // No se sobreescribe fechaHoraIngreso.
            throw new ConflictoException("Entrada ya utilizada");
        }
        if (entrada.getEstado() != EstadoEntrada.EMITIDA || entrada.getVenta() == null
                || entrada.getVenta().getEstado() != EstadoVenta.PAGADA) {
            throw new AccesoDenegadoException("Ticket no emitido / Falta de pago");
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!entrada.getEvento().getFechaHora().toLocalDate().equals(ahora.toLocalDate())) {
            throw new ConflictoException("La entrada no corresponde a un evento de hoy");
        }

        entrada.setEstado(EstadoEntrada.UTILIZADA);
        entrada.setFechaHoraIngreso(ahora);
        entradas.save(entrada); // Se confirma al finalizar la transaccion.

        return new AccesoResponse("Acceso Permitido", entrada.getId(),
            entrada.getEvento().getId(), entrada.getEvento().getNombre(),
            entrada.getSector().getId(), entrada.getEstado(), entrada.getFechaHoraIngreso());
    }
}
