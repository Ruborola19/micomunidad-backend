package com.micomunity.backend.repository;

import com.micomunity.backend.model.Votacion;
import com.micomunity.backend.model.Community;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VotacionRepository extends JpaRepository<Votacion, Long> {
    
    List<Votacion> findByComunidadAndFechaFinalAfter(Community comunidad, LocalDateTime now);

    List<Votacion> findByComunidadAndFechaFinalBefore(Community comunidad, LocalDateTime now);
}
