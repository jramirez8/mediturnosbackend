package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.EstadoListaEspera;
import com.ramirez.mediturnosback.model.ListaEsperaEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListaEsperaRepository extends JpaRepository<ListaEsperaEntry, Long> {
    List<ListaEsperaEntry> findByPacienteIdOrderByCreadoEnDesc(Long pacienteId);
    List<ListaEsperaEntry> findByEstadoOrderByCreadoEnAsc(EstadoListaEspera estado);
    Optional<ListaEsperaEntry> findFirstByProfesionalInstitucionIdAndEspecialidadIdAndEstadoOrderByCreadoEnAsc(Long profesionalInstitucionId, Long especialidadId, EstadoListaEspera estado);
}
