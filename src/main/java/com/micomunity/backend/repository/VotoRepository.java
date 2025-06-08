package com.micomunity.backend.repository;

import com.micomunity.backend.model.Voto;
import com.micomunity.backend.model.User;
import com.micomunity.backend.model.Votacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    Optional<Voto> findByVotanteAndVotacion(User votante, Votacion votacion);

    List<Voto> findByVotacion(Votacion votacion);
}
