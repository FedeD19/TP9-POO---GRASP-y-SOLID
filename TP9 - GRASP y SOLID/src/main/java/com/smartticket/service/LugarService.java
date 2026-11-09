package com.smartticket.service;

import com.smartticket.dto.LugarRequest;
import com.smartticket.dto.LugarResponse;
import com.smartticket.exception.RecursoNoEncontradoException;
import com.smartticket.exception.ConflictoException;
import com.smartticket.model.Lugar;
import com.smartticket.repository.LugarRepository;
import com.smartticket.repository.SectorRepository;
import com.smartticket.repository.EventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class LugarService {
    private final LugarRepository repository;
    private final SectorRepository sectores;
    private final EventoRepository eventos;

    public LugarService(LugarRepository repository,
                        SectorRepository sectores,
                        EventoRepository eventos) {
        this.repository = repository;
        this.sectores = sectores;
        this.eventos = eventos;
    }

    @Transactional
    public LugarResponse crear(LugarRequest datos) {
        Lugar lugar = new Lugar();
        lugar.setNombre(datos.nombre().trim());
        lugar.setDireccion(datos.direccion().trim());
        return convertir(repository.save(lugar));
    }

    @Transactional(readOnly = true)
    public List<LugarResponse> listar() {
        return repository.findAll().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public LugarResponse buscar(Long id) {
        return convertir(obtenerEntidad(id));
    }

    @Transactional
    public LugarResponse actualizar(Long id, LugarRequest datos) {
        Lugar lugar = obtenerEntidad(id);
        lugar.setNombre(datos.nombre().trim());
        lugar.setDireccion(datos.direccion().trim());
        return convertir(repository.save(lugar));
    }

    @Transactional
    public void eliminar(Long id) {
        Lugar lugar = obtenerEntidad(id);

        // No eliminamos un Lugar que todavia tenga Sectores o Eventos relacionados.
        if (sectores.existsByLugar_Id(id) || eventos.existsByLugar_Id(id)) {
            throw new ConflictoException(
                    "No se puede eliminar el lugar: tiene sectores o eventos asociados");
        }

        repository.delete(lugar);
        // Ejecuta el DELETE durante el servicio para detectar restricciones remanentes.
        repository.flush();
    }

    private Lugar obtenerEntidad(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Lugar no encontrado: " + id));
    }

    private LugarResponse convertir(Lugar lugar) {
        return new LugarResponse(lugar.getId(), lugar.getNombre(), lugar.getDireccion());
    }
}
