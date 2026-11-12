package com.smartticket.service;

import com.smartticket.dto.EntradaEmitidaResponse;
import com.smartticket.dto.PagoRequest;
import com.smartticket.dto.PagoResponse;
import com.smartticket.exception.ConflictoException;
import com.smartticket.exception.PagoDenegadoException;
import com.smartticket.exception.RecursoNoEncontradoException;
import com.smartticket.exception.SolicitudInvalidaException;
import com.smartticket.model.Cliente;
import com.smartticket.model.Entrada;
import com.smartticket.model.EstadoEntrada;
import com.smartticket.model.EstadoVenta;
import com.smartticket.model.PrecioSector;
import com.smartticket.model.Venta;
import com.smartticket.repository.ClienteRepository;
import com.smartticket.repository.EntradaRepository;
import com.smartticket.repository.EventoRepository;
import com.smartticket.repository.PrecioSectorRepository;
import com.smartticket.repository.VentaRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU 2: verifica inventario, cobra en pasarela ficticia y emite una compra atomica. */
@Service
public class PagoService {
    private final ClienteRepository clientes;
    private final EventoRepository eventos;
    private final PrecioSectorRepository precios;
    private final EntradaRepository entradas;
    private final VentaRepository ventas;
    private final PasarelaPago pasarela;

    public PagoService(ClienteRepository clientes, EventoRepository eventos,
                       PrecioSectorRepository precios, EntradaRepository entradas,
                       VentaRepository ventas, PasarelaPago pasarela) {
        this.clientes = clientes;
        this.eventos = eventos;
        this.precios = precios;
        this.entradas = entradas;
        this.ventas = ventas;
        this.pasarela = pasarela;
    }

    /**
     * Una transaccion abarca verificacion, cobro SIMULADO y emision.
     * El bloqueo del registro PrecioSector evita que dos hilos compren el
     * mismo stock a la vez. Para una pasarela real, NO se mantendria la
     * transaccion abierta durante una llamada de red: usar reservas y eventos.
     */
    @Transactional
    public PagoResponse iniciarPago(PagoRequest datos) {
        // Fallar rapido: no cobrar ni consultar inventario por cantidades invalidas.
        if (datos.cantidad() == null || datos.cantidad() <= 0) {
            throw new SolicitudInvalidaException(
                "La cantidad de entradas solicitadas debe ser mayor a cero");
        }
        Cliente cliente = clientes.findById(datos.clienteId())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Cliente no encontrado: " + datos.clienteId()));
        if (!eventos.existsById(datos.eventoId())) {
            throw new RecursoNoEncontradoException("Evento no encontrado: " + datos.eventoId());
        }

        // Es fundamental tomar el bloqueo ANTES de elegir las entradas.
        PrecioSector precio = precios.bloquearParaCompra(datos.eventoId(), datos.sectorId())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Sector no disponible para el evento: " + datos.sectorId()));

        List<Entrada> elegidas = entradas.findByEvento_IdAndSector_IdAndEstadoOrderByIdAsc(
            datos.eventoId(), datos.sectorId(), EstadoEntrada.DISPONIBLE,
            PageRequest.of(0, datos.cantidad()));
        if (elegidas.size() != datos.cantidad()) {
            // Antes de consultar la pasarela: no se cobra si otro cliente compro primero.
            throw new ConflictoException("Los lugares seleccionados ya no se encuentran disponibles");
        }

        // Nunca confiar en un total enviado por el cliente: calcularlo del precio persistido.
        BigDecimal total = precio.getPrecio().multiply(BigDecimal.valueOf(datos.cantidad()));
        if (!pasarela.cobrar(datos.tarjetaPrueba(), total)) {
            // No existe una Venta PAGADA ni una reserva por un rechazo.
            throw new PagoDenegadoException("Pago denegado");
        }

        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setTotal(total);
        venta.setEstado(EstadoVenta.PAGADA);
        ventas.save(venta);

        for (Entrada entrada : elegidas) {
            entrada.setVenta(venta);
            entrada.setEstado(EstadoEntrada.EMITIDA);
            // El QR fue generado al crear el evento (Etapa 3.1).
        }
        entradas.saveAll(elegidas);

        List<EntradaEmitidaResponse> emitidas = elegidas.stream()
            .map(e -> new EntradaEmitidaResponse(e.getId(), e.getEstado(), e.getCodigoQR()))
            .toList();
        return new PagoResponse(venta.getId(), venta.getEstado(), cliente.getId(),
            datos.eventoId(), datos.sectorId(), datos.cantidad(), total, emitidas);
    }
}
