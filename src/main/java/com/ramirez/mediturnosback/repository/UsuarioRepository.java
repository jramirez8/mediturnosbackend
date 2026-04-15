package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Usuario> findByTokenVerificacion(String tokenVerificacion);

    Optional<Usuario> findByTokenRecuperacion(String tokenRecuperacion);

    @Query("""
            select u from Usuario u
            left join u.paciente p
            left join u.profesional pr
            left join u.secretaria s
            where lower(u.email) = lower(:identificador)
               or p.dni = :identificador
               or pr.dni = :identificador
               or s.dni = :identificador
            """)
    Optional<Usuario> findByEmailOrPacienteDni(@Param("identificador") String identificador);
}
