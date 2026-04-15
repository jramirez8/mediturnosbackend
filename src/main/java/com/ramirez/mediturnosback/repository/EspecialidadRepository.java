package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {
    List<Especialidad> findByActivaTrueOrderByNombreAsc();
    Optional<Especialidad> findByNombreIgnoreCase(String nombre);
}
