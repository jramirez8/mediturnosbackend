package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.ObraSocial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObraSocialRepository extends JpaRepository<ObraSocial, Long> {
    List<ObraSocial> findByActivaTrueOrderByNombreAsc();
}
