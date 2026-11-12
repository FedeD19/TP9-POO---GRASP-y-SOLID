package com.smartticket.repository;

import com.smartticket.model.Entrada;
import com.smartticket.model.EstadoEntrada;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    /** Se seleccionan IDs estables, con limite, luego de obtener el bloqueo por sector. */
    List<Entrada> findByEvento_IdAndSector_IdAndEstadoOrderByIdAsc(
        Long eventoId, Long sectorId, EstadoEntrada estado, Pageable pagina);

    long countByEvento_IdAndSector_Id(Long eventoId, Long sectorId);
    long countByEvento_IdAndSector_IdAndEstado(Long eventoId, Long sectorId, EstadoEntrada estado);
    boolean existsBySector_Id(Long sectorId);
    boolean existsByEvento_IdAndEstadoNot(Long eventoId, EstadoEntrada estado);
    boolean existsByEvento_IdAndVentaIsNotNull(Long eventoId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Entrada e where e.evento.id = :eventoId")
    void eliminarPorEvento(@Param("eventoId") Long eventoId);
}
