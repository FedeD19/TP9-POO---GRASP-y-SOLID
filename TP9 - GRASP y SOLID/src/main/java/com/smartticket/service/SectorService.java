package com.smartticket.service;

import com.smartticket.dto.SectorRequest;
import com.smartticket.dto.SectorResponse;
import com.smartticket.exception.RecursoNoEncontradoException;
import com.smartticket.exception.ConflictoException;
import com.smartticket.model.Lugar;
import com.smartticket.model.Sector;
import com.smartticket.repository.LugarRepository;
import com.smartticket.repository.SectorRepository;
import com.smartticket.repository.PrecioSectorRepository;
import com.smartticket.repository.EntradaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SectorService {
    private final SectorRepository repository;
    private final LugarRepository lugares;
    private final PrecioSectorRepository precios;
    private final EntradaRepository entradas;

    public SectorService(SectorRepository repository, LugarRepository lugares,
                         PrecioSectorRepository precios, EntradaRepository entradas) {
        this.repository = repository;
        this.lugares = lugares;
        this.precios = precios;
        this.entradas = entradas;
    }

    @Transactional
    public SectorResponse crear(SectorRequest datos) {
        Sector sector = new Sector();
        aplicarDatos(sector, datos);
        return convertir(repository.save(sector));
    }

    @Transactional(readOnly = true)
    public List<SectorResponse> listar() {
        return repository.findAll().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public SectorResponse buscar(Long id) {
        return convertir(obtenerEntidad(id));
    }

    @Transactional
    public SectorResponse actualizar(Long id, SectorRequest datos) {
        Sector sector = obtenerEntidad(id);
        if ((!sector.getLugar().getId().equals(datos.lugarId())
                || sector.getCapacidadMaxima() != datos.capacidadMaxima())
                && (precios.existsBySector_Id(id) || entradas.existsBySector_Id(id))) {
            throw new ConflictoException(
                "No se puede cambiar la capacidad o el lugar de un sector usado por eventos");
        }
        aplicarDatos(sector, datos);
        return convertir(repository.save(sector));
    }

    @Transactional
    public void eliminar(Long id) {
        Sector sector = obtenerEntidad(id);
        if (precios.existsBySector_Id(id) || entradas.existsBySector_Id(id)) {
            throw new ConflictoException("No se puede eliminar un sector asociado a eventos");
        }
        repository.delete(sector);
        repository.flush();
    }

    private void aplicarDatos(Sector sector, SectorRequest datos) {
        Lugar lugar = lugares.findById(datos.lugarId()).orElseThrow(() ->
                new RecursoNoEncontradoException("Lugar no encontrado: " + datos.lugarId()));
        sector.setNombre(datos.nombre().trim());
        sector.setCapacidadMaxima(datos.capacidadMaxima());
        sector.setLugar(lugar);
    }

    private Sector obtenerEntidad(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Sector no encontrado: " + id));
    }

    private SectorResponse convertir(Sector sector) {
        return new SectorResponse(sector.getId(), sector.getNombre(),
                sector.getCapacidadMaxima(), sector.getLugar().getId());
    }
}
