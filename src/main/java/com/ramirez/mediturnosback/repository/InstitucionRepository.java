package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.Institucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstitucionRepository extends JpaRepository<Institucion, Long> {
    List<Institucion> findByActivaTrueOrderByNombreAsc();
    Optional<Institucion> findByNombreIgnoreCase(String nombre);
}
