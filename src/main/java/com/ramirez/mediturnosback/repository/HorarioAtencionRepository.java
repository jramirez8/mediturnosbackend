package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {
    List<HorarioAtencion> findByProfesionalInstitucionIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(Long profesionalInstitucionId);
}
