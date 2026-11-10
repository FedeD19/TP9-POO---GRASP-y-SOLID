package com.smartticket.repository;

import com.smartticket.model.PrecioSector;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PrecioSectorRepository extends JpaRepository<PrecioSector, Long> {
    List<PrecioSector> findByEvento_IdOrderBySector_Id(Long eventoId);
    boolean existsBySector_Id(Long sectorId);
    java.util.Optional<PrecioSector> findByEvento_IdAndSector_Id(Long eventoId, Long sectorId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PrecioSector p where p.evento.id = :eventoId")
    void eliminarPorEvento(@Param("eventoId") Long eventoId);
}
