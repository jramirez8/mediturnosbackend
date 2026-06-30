package com.ramirez.mediturnosback.config;

import com.ramirez.mediturnosback.model.Especialidad;
import com.ramirez.mediturnosback.model.Profesional;
import com.ramirez.mediturnosback.model.ProfesionalInstitucion;
import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.model.Usuario;
import com.ramirez.mediturnosback.repository.EspecialidadRepository;
import com.ramirez.mediturnosback.repository.InstitucionRepository;
import com.ramirez.mediturnosback.repository.ProfesionalRepository;
import com.ramirez.mediturnosback.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import org.springframework.core.annotation.Order;

@Configuration
@Order(2)
public class ProfesionalDataLoader {

    @Bean
    @Order(3)
    CommandLineRunner seedProfesionales(ProfesionalRepository profesionalRepository,
                                        UsuarioRepository usuarioRepository,
                                        InstitucionRepository institucionRepository,
                                        EspecialidadRepository especialidadRepository,
                                        PasswordEncoder passwordEncoder,
                                        @Value("${app.seed.demo-professionals:false}") boolean seedDemoProfessionals) {
        return args -> {
            if (!seedDemoProfessionals) return;
            if (profesionalRepository.count() > 0) return;

            crearProfesional(profesionalRepository, usuarioRepository, passwordEncoder, institucionRepository, especialidadRepository,
                    "Ana", "Lopez", "20111222", "MN-1001", "2284-410001", "cardio1@mediturnos.local",
                    Set.of("Cardiología"),
                    List.of("Clínica del Centro", "Sanatorio Tandil"));
            crearProfesional(profesionalRepository, usuarioRepository, passwordEncoder, institucionRepository, especialidadRepository,
                    "Marcos", "Suarez", "20222333", "MN-1002", "2284-410002", "cardio2@mediturnos.local",
                    Set.of("Cardiología"),
                    List.of("Hospital Municipal"));
            crearProfesional(profesionalRepository, usuarioRepository, passwordEncoder, institucionRepository, especialidadRepository,
                    "Lucia", "Paredes", "20333444", "MN-2001", "2284-410003", "pediatria1@mediturnos.local",
                    Set.of("Pediatría"),
                    List.of("Hospital Municipal", "Consultorios del Parque"));
            crearProfesional(profesionalRepository, usuarioRepository, passwordEncoder, institucionRepository, especialidadRepository,
                    "Federico", "Mendez", "20444555", "MN-3001", "2284-410004", "clinica1@mediturnos.local",
                    Set.of("Clínica Médica"),
                    List.of("Clínica del Centro"));
            crearProfesional(profesionalRepository, usuarioRepository, passwordEncoder, institucionRepository, especialidadRepository,
                    "Paula", "Ferrari", "20555666", "MN-4001", "2284-410005", "derma1@mediturnos.local",
                    Set.of("Dermatología"),
                    List.of("Consultorios del Parque"));
            crearProfesional(profesionalRepository, usuarioRepository, passwordEncoder, institucionRepository, especialidadRepository,
                    "Sofia", "Quiroga", "20666777", "MN-5001", "2284-410006", "gine1@mediturnos.local",
                    Set.of("Ginecología"),
                    List.of("Instituto Integral de la Mujer"));
        };
    }

    @SuppressWarnings("java:S107")
    private void crearProfesional(ProfesionalRepository profesionalRepository,
                                  UsuarioRepository usuarioRepository,
                                  PasswordEncoder passwordEncoder,
                                  InstitucionRepository institucionRepository,
                                  EspecialidadRepository especialidadRepository,
                                  String nombre,
                                  String apellido,
                                  String dni,
                                  String matricula,
                                  String telefono,
                                  String email,
                                  Set<String> especialidades,
                                  List<String> instituciones) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) return;

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode("Profesional123"));
        usuario.setRol(RolUsuario.PROFESSIONAL);
        usuario.setEmailVerificado(true);
        usuario.setActivo(true);

        Profesional profesional = new Profesional();
        profesional.setNombre(nombre);
        profesional.setApellido(apellido);
        profesional.setDni(dni);
        profesional.setMatricula(matricula);
        profesional.setTelefono(telefono);
        profesional.setUsuario(usuario);
        usuario.setProfesional(profesional);

        especialidades.forEach(nombreEspecialidad -> {
            Especialidad especialidad = especialidadRepository.findByNombreIgnoreCase(nombreEspecialidad)
                    .orElseGet(() -> {
                        Especialidad newEsp = new Especialidad();
                        newEsp.setNombre(nombreEspecialidad);
                        newEsp.setActiva(true);
                        return especialidadRepository.save(newEsp);
                    });
            profesional.getEspecialidades().add(especialidad);
        });

        instituciones.forEach(nombreInstitucion -> {
            var institucion = institucionRepository.findByNombreIgnoreCase(nombreInstitucion)
                    .orElseGet(() -> {
                        var newInst = new com.ramirez.mediturnosback.model.Institucion();
                        newInst.setNombre(nombreInstitucion);
                        newInst.setTipo(com.ramirez.mediturnosback.model.TipoInstitucion.OTRO);
                        newInst.setDireccion("Dirección autogenerada");
                        newInst.setActiva(true);
                        return institucionRepository.save(newInst);
                    });
            ProfesionalInstitucion pi = new ProfesionalInstitucion();
            pi.setProfesional(profesional);
            pi.setInstitucion(institucion);
            pi.setTelefonoEnSede(telefono);
            pi.setActivo(true);
            profesional.getSedes().add(pi);
        });

        profesionalRepository.save(profesional);
    }
}
