package com.smartticket.repository;

import com.smartticket.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    boolean existsByLugar_Id(Long lugarId);

}
