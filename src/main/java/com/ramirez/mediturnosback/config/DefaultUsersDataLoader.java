package com.ramirez.mediturnosback.config;

import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.model.Usuario;
import com.ramirez.mediturnosback.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Order(3)
public class DefaultUsersDataLoader {

    @Bean
    @Order(4)
    CommandLineRunner seedAdmin(UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin.email:admin@mediturnos.net.ar}") String adminEmail,
                                @Value("${app.admin.password:}") String adminPassword) {
        return args -> {
            String normalizedEmail = adminEmail == null || adminEmail.isBlank()
                    ? "admin@mediturnos.net.ar"
                    : adminEmail.trim().toLowerCase();

            if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                return;
            }
            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "Falta APP_ADMIN_PASSWORD. Es obligatoria para crear el administrador inicial."
                );
            }
            if (adminPassword.length() < 8) {
                throw new IllegalStateException("APP_ADMIN_PASSWORD debe tener al menos 8 caracteres.");
            }

            Usuario admin = new Usuario();
            admin.setEmail(normalizedEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRol(RolUsuario.ADMIN);
            admin.setEmailVerificado(true);
            admin.setActivo(true);
            admin.setTwoFactorEmailEnabled(false);
            usuarioRepository.save(admin);
        };
    }
}
