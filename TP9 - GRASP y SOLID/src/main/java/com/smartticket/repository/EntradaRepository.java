package com.smartticket.repository;

import com.smartticket.model.Entrada;
import com.smartticket.model.EstadoEntrada;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    /** Explorador: paginacion por evento; no se carga un inventario completo en memoria. */
    Page<Entrada> findByEvento_Id(Long eventoId, Pageable pagina);
    long countByVenta_Id(Long ventaId);

    /** Un QR se escanea bajo bloqueo exclusivo para impedir ingreso duplicado concurrente. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrada e where e.codigoQR = :codigoQR")
    Optional<Entrada> bloquearPorCodigoQR(@Param("codigoQR") String codigoQR);

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
