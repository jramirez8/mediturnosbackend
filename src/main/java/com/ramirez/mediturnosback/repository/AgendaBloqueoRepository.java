package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.AgendaBloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendaBloqueoRepository extends JpaRepository<AgendaBloqueo, Long> {
    List<AgendaBloqueo> findByProfesionalInstitucionIdOrderByFechaDesdeAsc(Long profesionalInstitucionId);

    @Query("select b from AgendaBloqueo b where b.profesionalInstitucion.id = :piId and b.fechaDesde <= :to and b.fechaHasta >= :from")
    List<AgendaBloqueo> findBloqueosActivos(@Param("piId") Long profesionalInstitucionId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
