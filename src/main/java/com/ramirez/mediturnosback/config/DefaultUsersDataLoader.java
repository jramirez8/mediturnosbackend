package com.ramirez.mediturnosback.config;

import com.ramirez.mediturnosback.model.Institucion;
import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.model.Secretaria;
import com.ramirez.mediturnosback.model.TipoInstitucion;
import com.ramirez.mediturnosback.model.Usuario;
import com.ramirez.mediturnosback.repository.InstitucionRepository;
import com.ramirez.mediturnosback.repository.SecretariaRepository;
import com.ramirez.mediturnosback.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Order(3)
public class DefaultUsersDataLoader {

    @Bean
    CommandLineRunner seedDefaultUsers(UsuarioRepository usuarioRepository,
                                       SecretariaRepository secretariaRepository,
                                       InstitucionRepository institucionRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            seedAdmin(usuarioRepository, passwordEncoder);
            seedSecretaria(usuarioRepository, secretariaRepository, institucionRepository, passwordEncoder,
                    "secretaria1@mediturnos.local", "Secretaria123", "Silvia", "Gomez", "27111222",
                    "2284-410101", "Hospital Municipal");
            seedSecretaria(usuarioRepository, secretariaRepository, institucionRepository, passwordEncoder,
                    "secretaria2@mediturnos.local", "Secretaria123", "Carla", "Perez", "27222333",
                    "2284-410102", "Clínica del Centro");
        };
    }

    private void seedAdmin(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        final String email = "admin@mediturnos.local";
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode("Admin1234"));
        admin.setRol(RolUsuario.ADMIN);
        admin.setEmailVerificado(true);
        admin.setActivo(true);
        usuarioRepository.save(admin);
    }

    private void seedSecretaria(UsuarioRepository usuarioRepository,
                                SecretariaRepository secretariaRepository,
                                InstitucionRepository institucionRepository,
                                PasswordEncoder passwordEncoder,
                                String email,
                                String password,
                                String nombre,
                                String apellido,
                                String dni,
                                String telefono,
                                String nombreInstitucion) {
        if (usuarioRepository.existsByEmailIgnoreCase(email) || secretariaRepository.existsByDni(dni)) {
            return;
        }

        Institucion institucion = institucionRepository.findByNombreIgnoreCase(nombreInstitucion)
                .orElseGet(() -> {
                    Institucion nueva = new Institucion();
                    nueva.setNombre(nombreInstitucion);
                    nueva.setTipo(TipoInstitucion.CONSULTORIO);
                    nueva.setDireccion("Dirección autogenerada");
                    nueva.setTelefono(telefono);
                    nueva.setWhatsapp(telefono);
                    nueva.setActiva(true);
                    return institucionRepository.save(nueva);
                });

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setRol(RolUsuario.SECRETARY);
        usuario.setEmailVerificado(true);
        usuario.setActivo(true);

        Secretaria secretaria = new Secretaria();
        secretaria.setNombre(nombre);
        secretaria.setApellido(apellido);
        secretaria.setDni(dni);
        secretaria.setTelefono(telefono);
        secretaria.setInstitucion(institucion);
        secretaria.setActiva(true);
        secretaria.setUsuario(usuario);
        usuario.setSecretaria(secretaria);

        secretariaRepository.save(secretaria);
    }
}
