package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.PacienteDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PacienteDocumentoRepository extends JpaRepository<PacienteDocumento, Long> {
    List<PacienteDocumento> findByPacienteIdAndArchivadoFalseOrderByCreadoEnDesc(Long pacienteId);
    List<PacienteDocumento> findByPacienteIdOrderByCreadoEnDesc(Long pacienteId);
}
