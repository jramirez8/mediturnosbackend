package com.ramirez.mediturnosback.config;

import com.ramirez.mediturnosback.model.Institucion;
import com.ramirez.mediturnosback.model.ObraSocial;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.model.Secretaria;
import com.ramirez.mediturnosback.model.TipoInstitucion;
import com.ramirez.mediturnosback.model.TipoSangre;
import com.ramirez.mediturnosback.model.Usuario;
import com.ramirez.mediturnosback.repository.InstitucionRepository;
import com.ramirez.mediturnosback.repository.ObraSocialRepository;
import com.ramirez.mediturnosback.repository.PacienteRepository;
import com.ramirez.mediturnosback.repository.SecretariaRepository;
import com.ramirez.mediturnosback.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
@Order(3)
public class DefaultUsersDataLoader {

    @Bean
    @Order(4)
    CommandLineRunner seedDefaultUsers(UsuarioRepository usuarioRepository,
                                       SecretariaRepository secretariaRepository,
                                       PacienteRepository pacienteRepository,
                                       InstitucionRepository institucionRepository,
                                       ObraSocialRepository obraSocialRepository,
                                       PasswordEncoder passwordEncoder,
                                       @Value("${app.admin.email:admin@mediturnos.net.ar}") String adminEmail,
                                       @Value("${app.admin.password:11223344}") String adminPassword,
                                       @Value("${app.seed.demo-users:false}") boolean seedDemoUsers) {
        return args -> {
            seedAdmin(usuarioRepository, passwordEncoder, adminEmail, adminPassword);
            if (!seedDemoUsers) return;
            seedPaciente(usuarioRepository, pacienteRepository, institucionRepository, obraSocialRepository, passwordEncoder);
            seedSecretaria(usuarioRepository, secretariaRepository, institucionRepository, passwordEncoder,
                    "secretaria1@mediturnos.local", "Secretaria123", "Silvia", "Gomez", "27111222",
                    "2284-410101", "Hospital Municipal");
            seedSecretaria(usuarioRepository, secretariaRepository, institucionRepository, passwordEncoder,
                    "secretaria2@mediturnos.local", "Secretaria123", "Carla", "Perez", "27222333",
                    "2284-410102", "Clínica del Centro");
        };
    }

    private void seedAdmin(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, String email, String password) {
        String normalizedEmail = email == null || email.isBlank() ? "admin@mediturnos.net.ar" : email.trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(password == null || password.isBlank() ? "11223344" : password));
        admin.setRol(RolUsuario.ADMIN);
        admin.setEmailVerificado(true);
        admin.setActivo(true);
        admin.setTwoFactorEmailEnabled(false);
        usuarioRepository.save(admin);
    }

    private void seedPaciente(UsuarioRepository usuarioRepository,
                              PacienteRepository pacienteRepository,
                              InstitucionRepository institucionRepository,
                              ObraSocialRepository obraSocialRepository,
                              PasswordEncoder passwordEncoder) {
        final String email = "paciente@mediturnos.local";
        final String dni = "30111222";
        if (usuarioRepository.existsByEmailIgnoreCase(email) || pacienteRepository.existsByDni(dni)) {
            return;
        }

        ObraSocial obraSocial = obraSocialRepository.findAll().stream().findFirst().orElseGet(() -> {
            ObraSocial nueva = new ObraSocial();
            nueva.setNombre("Particular");
            nueva.setCodigo("PART");
            nueva.setActiva(true);
            return obraSocialRepository.save(nueva);
        });

        Institucion institucion = institucionRepository.findByNombreIgnoreCase("Hospital Municipal").orElseGet(() -> {
            Institucion nueva = new Institucion();
            nueva.setNombre("Hospital Municipal");
            nueva.setTipo(TipoInstitucion.HOSPITAL);
            nueva.setDireccion("Maipú 321, Tandil");
            nueva.setTelefono("2284-410000");
            nueva.setWhatsapp("2284-15000001");
            nueva.setActiva(true);
            return institucionRepository.save(nueva);
        });

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode("Paciente1234"));
        usuario.setRol(RolUsuario.PATIENT);
        usuario.setEmailVerificado(true);
        usuario.setActivo(true);

        Paciente paciente = new Paciente();
        paciente.setNombre("Juan");
        paciente.setApellido("Ramírez");
        paciente.setDni(dni);
        paciente.setFechaNacimiento(LocalDate.of(1998, 4, 12));
        paciente.setTelefono("2284-555555");
        paciente.setTipoSangre(TipoSangre.O_POSITIVO);
        paciente.setNumeroCarnet("DEMO-001");
        paciente.setNumeroHistoriaClinica("HC-DEMO-001");
        paciente.setObraSocial(obraSocial);
        paciente.setInstitucionCabecera(institucion);
        paciente.setActivo(true);
        paciente.setUsuario(usuario);
        usuario.setPaciente(paciente);

        pacienteRepository.save(paciente);
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
