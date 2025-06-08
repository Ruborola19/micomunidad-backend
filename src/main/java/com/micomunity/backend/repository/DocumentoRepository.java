package com.micomunity.backend.repository;

import com.micomunity.backend.model.Documento;
import com.micomunity.backend.model.Community;
import com.micomunity.backend.model.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    @Query("SELECT d FROM Documento d " +
           "LEFT JOIN FETCH d.autor " +
           "LEFT JOIN FETCH d.comunidad " +
           "WHERE d.comunidad = :comunidad " +
           "AND d.tipo = :tipo " +
           "ORDER BY d.fechaPublicacion DESC")
    Page<Documento> findByComunidadAndTipoOrderByFechaPublicacionDesc(
            @Param("comunidad") Community comunidad,
            @Param("tipo") TipoDocumento tipo,
            Pageable pageable
    );

    @Query("SELECT d FROM Documento d " +
           "LEFT JOIN FETCH d.autor " +
           "LEFT JOIN FETCH d.comunidad " +
           "WHERE d.comunidad = :comunidad " +
           "AND d.tipo = :tipo " +
           "AND (:dia IS NULL OR DAY(d.fechaPublicacion) = :dia) " +
           "AND (:mes IS NULL OR MONTH(d.fechaPublicacion) = :mes) " +
           "AND (:anio IS NULL OR YEAR(d.fechaPublicacion) = :anio) " +
           "ORDER BY d.fechaPublicacion DESC")
    Page<Documento> findByComunidadAndTipoWithDateFilters(
            @Param("comunidad") Community comunidad,
            @Param("tipo") TipoDocumento tipo,
            @Param("dia") Integer dia,
            @Param("mes") Integer mes,
            @Param("anio") Integer anio,
            Pageable pageable
    );
}
