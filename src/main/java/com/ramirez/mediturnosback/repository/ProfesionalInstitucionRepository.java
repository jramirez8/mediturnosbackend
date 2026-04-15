package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.ProfesionalInstitucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfesionalInstitucionRepository extends JpaRepository<ProfesionalInstitucion, Long> {

    @Query("""
            select distinct pi from ProfesionalInstitucion pi
            join fetch pi.profesional p
            join fetch pi.institucion i
            join fetch p.especialidades e
            where pi.activo = true and p.activo = true and i.activa = true
              and (:especialidad is null or lower(e.nombre) = lower(:especialidad))
              and (:q is null or lower(i.nombre) like lower(concat('%', :q, '%'))
                   or lower(p.nombre) like lower(concat('%', :q, '%'))
                   or lower(p.apellido) like lower(concat('%', :q, '%')))
            order by p.apellido asc, p.nombre asc
            """)
    List<ProfesionalInstitucion> buscarDisponibles(@Param("especialidad") String especialidad,
                                                   @Param("q") String q);

    List<ProfesionalInstitucion> findByProfesionalIdAndActivoTrue(Long profesionalId);
}
