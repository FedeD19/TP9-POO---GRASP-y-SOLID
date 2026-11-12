package com.smartticket.repository;

import com.smartticket.model.PrecioSector;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PrecioSectorRepository extends JpaRepository<PrecioSector, Long> {
    List<PrecioSector> findByEvento_IdOrderBySector_Id(Long eventoId);
    boolean existsBySector_Id(Long sectorId);
    java.util.Optional<PrecioSector> findByEvento_IdAndSector_Id(Long eventoId, Long sectorId);

    /** Serializa las compras concurrentes del MISMO evento y sector. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PrecioSector p where p.evento.id = :eventoId and p.sector.id = :sectorId")
    java.util.Optional<PrecioSector> bloquearParaCompra(
        @Param("eventoId") Long eventoId, @Param("sectorId") Long sectorId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PrecioSector p where p.evento.id = :eventoId")
    void eliminarPorEvento(@Param("eventoId") Long eventoId);
}
