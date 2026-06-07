package com.ramirez.mediturnosback.config;

import com.ramirez.mediturnosback.model.ObraSocial;
import com.ramirez.mediturnosback.repository.ObraSocialRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
@Order(2)
public class ObraSocialDataLoader {

    @Bean
    @Order(2)
    public CommandLineRunner seedObrasSociales(ObraSocialRepository obraSocialRepository) {
        return args -> {
            if (obraSocialRepository.count() > 0) {
                return;
            }

            List<ObraSocial> obras = List.of(
                    crear("Particular", "PART"),
                    crear("IOMA", "IOMA"),
                    crear("PAMI", "PAMI"),
                    crear("OSDE", "OSDE"),
                    crear("Swiss Medical", "SWISS"),
                    crear("Galeno", "GALENO"),
                    crear("Medifé", "MEDIFE"),
                    crear("Sancor Salud", "SANCOR"),
                    crear("OSECAC", "OSECAC"),
                    crear("Jerárquicos Salud", "JERARQ"),
                    crear("Avalian", "AVALIAN"),
                    crear("Prevención Salud", "PREV"),
                    crear("OSDEPYM", "OSDEPYM"),
                    crear("DOSEM", "DOSEM")
            );

            obraSocialRepository.saveAll(obras);
        };
    }

    private ObraSocial crear(String nombre, String codigo) {
        ObraSocial obraSocial = new ObraSocial();
        obraSocial.setNombre(nombre);
        obraSocial.setCodigo(codigo);
        obraSocial.setActiva(true);
        return obraSocial;
    }
}
