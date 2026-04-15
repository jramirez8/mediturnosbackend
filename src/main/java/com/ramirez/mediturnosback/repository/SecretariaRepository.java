package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.Secretaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecretariaRepository extends JpaRepository<Secretaria, Long> {
    Optional<Secretaria> findByUsuario_Id(Long usuarioId);
    boolean existsByDni(String dni);
}
