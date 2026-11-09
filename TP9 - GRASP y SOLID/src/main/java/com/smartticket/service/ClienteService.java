package com.smartticket.service;

import com.smartticket.dto.ClienteRequest;
import com.smartticket.dto.ClienteResponse;
import com.smartticket.exception.ConflictoException;
import com.smartticket.exception.RecursoNoEncontradoException;
import com.smartticket.model.Cliente;
import com.smartticket.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
public class ClienteService {
    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest datos) {
        String email = normalizarEmail(datos.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictoException("Ya existe un cliente con ese email");
        }
        Cliente cliente = new Cliente();
        cliente.setNombre(datos.nombre().trim());
        cliente.setEmail(email);
        return convertir(repository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return repository.findAll().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        return convertir(obtenerEntidad(id));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest datos) {
        Cliente cliente = obtenerEntidad(id);
        String email = normalizarEmail(datos.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictoException("Ya existe un cliente con ese email");
        }
        cliente.setNombre(datos.nombre().trim());
        cliente.setEmail(email);
        return convertir(repository.save(cliente));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(obtenerEntidad(id));
        repository.flush();
    }

    private Cliente obtenerEntidad(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ClienteResponse convertir(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getEmail());
    }
}
