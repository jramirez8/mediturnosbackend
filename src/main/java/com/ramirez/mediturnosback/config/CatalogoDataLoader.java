package com.ramirez.mediturnosback.config;

import com.ramirez.mediturnosback.model.Especialidad;
import com.ramirez.mediturnosback.model.Institucion;
import com.ramirez.mediturnosback.model.TipoInstitucion;
import com.ramirez.mediturnosback.repository.EspecialidadRepository;
import com.ramirez.mediturnosback.repository.InstitucionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import org.springframework.core.annotation.Order;

@Configuration
@Order(1)
public class CatalogoDataLoader {

    @Bean
    @Order(1)
    CommandLineRunner seedCatalogos(InstitucionRepository institucionRepository,
                                    EspecialidadRepository especialidadRepository) {
        return args -> {
            if (institucionRepository.count() == 0) {
                institucionRepository.saveAll(List.of(
                        crearInstitucion("Hospital Municipal", TipoInstitucion.HOSPITAL, "Maipú 321, Tandil", "2284-410000", "2284-15000001"),
                        crearInstitucion("Clínica del Centro", TipoInstitucion.CLINICA, "Av. Colón 123, Tandil", "2284-420000", "2284-15000002"),
                        crearInstitucion("Sanatorio Tandil", TipoInstitucion.SANATORIO, "Pinto 456, Tandil", "2284-430000", "2284-15000003"),
                        crearInstitucion("Consultorios del Parque", TipoInstitucion.CONSULTORIO, "9 de Julio 654, Tandil", "2284-440000", "2284-15000004"),
                        crearInstitucion("Instituto Integral de la Mujer", TipoInstitucion.CENTRO_MEDICO, "Mitre 852, Tandil", "2284-450000", "2284-15000005")
                ));
            }
            if (especialidadRepository.count() == 0) {
                especialidadRepository.saveAll(List.of(
                        crearEspecialidad("Clínica Médica"),
                        crearEspecialidad("Cardiología"),
                        crearEspecialidad("Pediatría"),
                        crearEspecialidad("Dermatología"),
                        crearEspecialidad("Ginecología"),
                        crearEspecialidad("Traumatología"),
                        crearEspecialidad("Neurología"),
                        crearEspecialidad("Oftalmología")
                ));
            }
        };
    }

    private Institucion crearInstitucion(String nombre, TipoInstitucion tipo, String direccion, String telefono, String whatsapp) {
        Institucion institucion = new Institucion();
        institucion.setNombre(nombre);
        institucion.setTipo(tipo);
        institucion.setDireccion(direccion);
        institucion.setTelefono(telefono);
        institucion.setWhatsapp(whatsapp);
        institucion.setActiva(true);
        return institucion;
    }

    private Especialidad crearEspecialidad(String nombre) {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(nombre);
        especialidad.setActiva(true);
        return especialidad;
    }
}
