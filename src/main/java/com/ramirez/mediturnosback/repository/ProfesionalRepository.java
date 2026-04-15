package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {
    boolean existsByMatricula(String matricula);
    Optional<Profesional> findByUsuario_Id(Long usuarioId);

    @Query("select distinct e.nombre from Profesional p join p.especialidades e where p.activo = true and e.activa = true order by e.nombre asc")
    List<String> listarNombresEspecialidades();
}
