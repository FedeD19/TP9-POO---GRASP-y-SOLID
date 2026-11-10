package com.smartticket.repository;

import com.smartticket.model.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {
    boolean existsByLugar_Id(Long lugarId);
    java.util.List<Sector> findByLugar_IdOrderById(Long lugarId);

}
