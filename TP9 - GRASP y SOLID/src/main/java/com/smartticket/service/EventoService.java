package com.smartticket.service;

import com.smartticket.dto.*;
import com.smartticket.exception.*;
import com.smartticket.model.*;
import com.smartticket.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class EventoService {
    private static final int TAMANO_LOTE = 200;

    private final EventoRepository repository;
    private final LugarRepository lugares;
    private final SectorRepository sectores;
    private final PrecioSectorRepository precios;
    private final EntradaRepository entradas;

    public EventoService(EventoRepository repository, LugarRepository lugares,
                         SectorRepository sectores, PrecioSectorRepository precios,
                         EntradaRepository entradas) {
        this.repository = repository;
        this.lugares = lugares;
        this.sectores = sectores;
        this.precios = precios;
        this.entradas = entradas;
    }

    /** Una transaccion: o se guarda evento + precios + inventario, o no se guarda nada. */
    @Transactional
    public EventoResponse crear(EventoCreacionRequest datos) {
        Lugar lugar = obtenerLugar(datos.lugarId());
        List<Sector> sectoresDelLugar = sectores.findByLugar_IdOrderById(lugar.getId());
        Map<Long, BigDecimal> listaPrecios = validarPrecios(datos.preciosSectores(), sectoresDelLugar);

        Evento evento = new Evento();
        evento.setNombre(datos.nombre().trim());
        evento.setFechaHora(datos.fechaHora());
        evento.setLugar(lugar);
        repository.save(evento);

        for (Sector sector : sectoresDelLugar) {
            precios.save(new PrecioSector(evento, sector, listaPrecios.get(sector.getId())));
            generarEntradas(evento, sector);
        }
        return convertir(evento);
    }

    /** Rechaza precios ausentes, repetidos, negativos o sectores de otro lugar. */
    private Map<Long, BigDecimal> validarPrecios(List<PrecioSectorRequest> solicitados,
                                                  List<Sector> disponibles) {
        if (disponibles.isEmpty()) {
            throw new SolicitudInvalidaException("El lugar debe tener al menos un sector antes de crear el evento");
        }
        Map<Long, BigDecimal> porSector = new HashMap<>();
        for (PrecioSectorRequest item : solicitados) {
            // Bean Validation comprueba los atributos, pero mantenemos esta comprobacion de seguridad.
            if (item == null || item.sectorId() == null || item.precio() == null
                    || item.precio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new SolicitudInvalidaException("Debe indicar ID de sector y precio mayor a cero");
            }
            if (porSector.putIfAbsent(item.sectorId(), item.precio()) != null) {
                throw new SolicitudInvalidaException("Sector repetido en la lista de precios: " + item.sectorId());
            }
        }
        Set<Long> sectorIds = new HashSet<>();
        for (Sector sector : disponibles) sectorIds.add(sector.getId());
        if (!porSector.keySet().equals(sectorIds)) {
            throw new SolicitudInvalidaException(
                "Debe indicar exactamente un precio para cada sector del lugar (sin sectores externos)");
        }
        return porSector;
    }

    /** QR unico pre-generado, pero NO autoriza acceso hasta que la entrada se emita en HU 2. */
    private void generarEntradas(Evento evento, Sector sector) {
        List<Entrada> lote = new ArrayList<>(TAMANO_LOTE);
        for (int i = 0; i < sector.getCapacidadMaxima(); i++) {
            Entrada entrada = new Entrada();
            entrada.setEvento(evento);
            entrada.setSector(sector);
            entrada.setEstado(EstadoEntrada.DISPONIBLE);
            entrada.setCodigoQR(UUID.randomUUID().toString());
            lote.add(entrada);
            if (lote.size() == TAMANO_LOTE) {
                entradas.saveAll(lote);
                lote.clear();
            }
        }
        if (!lote.isEmpty()) entradas.saveAll(lote);
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listar() {
        return repository.findAll().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse buscar(Long id) { return convertir(obtenerEntidad(id)); }

    @Transactional(readOnly = true)
    public InventarioEventoResponse inventario(Long id) {
        Evento evento = obtenerEntidad(id);
        List<InventarioSectorResponse> detalle = precios.findByEvento_IdOrderBySector_Id(id)
            .stream().map(p -> {
                Long sectorId = p.getSector().getId();
                Sector s = p.getSector();
                return new InventarioSectorResponse(
                    sectorId, s.getNombre(), s.getCapacidadMaxima(), p.getPrecio(),
                    entradas.countByEvento_IdAndSector_Id(id, sectorId),
                    entradas.countByEvento_IdAndSector_IdAndEstado(id, sectorId, EstadoEntrada.DISPONIBLE),
                    entradas.countByEvento_IdAndSector_IdAndEstado(id, sectorId, EstadoEntrada.RESERVADA),
                    entradas.countByEvento_IdAndSector_IdAndEstado(id, sectorId, EstadoEntrada.EMITIDA),
                    entradas.countByEvento_IdAndSector_IdAndEstado(id, sectorId, EstadoEntrada.UTILIZADA));
            }).toList();
        return new InventarioEventoResponse(evento.getId(), evento.getNombre(), detalle);
    }

    /** Mantiene PUT de la Etapa 2: editar nombre/fecha si no se cambia el lugar. */
    @Transactional
    public EventoResponse actualizar(Long id, EventoRequest datos) {
        Evento evento = obtenerEntidad(id);
        if (!evento.getLugar().getId().equals(datos.lugarId())) {
            throw new ConflictoException(
                "No se puede cambiar el lugar de un evento con entradas y precios ya generados");
        }
        evento.setNombre(datos.nombre().trim());
        evento.setFechaHora(datos.fechaHora());
        return convertir(repository.save(evento));
    }

    /** Borrado controlado: permitido solo si todas las entradas siguen sin venderse. */
    @Transactional
    public void eliminar(Long id) {
        obtenerEntidad(id);
        if (entradas.existsByEvento_IdAndEstadoNot(id, EstadoEntrada.DISPONIBLE)
                || entradas.existsByEvento_IdAndVentaIsNotNull(id)) {
            throw new ConflictoException("No se puede eliminar un evento con entradas reservadas, emitidas o utilizadas");
        }
        entradas.eliminarPorEvento(id);
        precios.eliminarPorEvento(id);
        // Las consultas DELETE de repositorio limpian el contexto JPA.
        // Recuperamos el registro dentro de la transaccion antes de borrarlo.
        repository.delete(obtenerEntidad(id));
        repository.flush();
    }

    private Lugar obtenerLugar(Long id) {
        return lugares.findById(id).orElseThrow(() ->
            new RecursoNoEncontradoException("Lugar no encontrado: " + id));
    }

    private Evento obtenerEntidad(Long id) {
        return repository.findById(id).orElseThrow(() ->
            new RecursoNoEncontradoException("Evento no encontrado: " + id));
    }

    private EventoResponse convertir(Evento evento) {
        return new EventoResponse(evento.getId(), evento.getNombre(),
            evento.getFechaHora(), evento.getLugar().getId());
    }
}
