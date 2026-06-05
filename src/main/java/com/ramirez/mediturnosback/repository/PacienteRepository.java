package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByDni(String dni);
    boolean existsByDni(String dni);
    boolean existsByNumeroHistoriaClinica(String numeroHistoriaClinica);
    boolean existsByTelefono(String telefono);
    Optional<Paciente> findByUsuario_Id(Long usuarioId);
}
