package com.ramirez.mediturnosback.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Order(0)
public class DatabaseResetDataLoader {
    private static final Logger log = LoggerFactory.getLogger(DatabaseResetDataLoader.class);

    @Bean
    @Order(0)
    CommandLineRunner resetDatabaseIfRequested(JdbcTemplate jdbcTemplate,
                                                @Value("${mediturnos.reset-db:false}") boolean resetDb) {
        return args -> {
            if (!resetDb) return;

            log.warn("MEDITURNOS_RESET_DB=true: se va a limpiar la base y reconstruir datos iniciales. Quitá esta variable después del primer deploy.");
            disableFkChecks(jdbcTemplate);
            String[] tables = {
                    "turno_adjuntos",
                    "turnos_feedback",
                    "consultas",
                    "lista_espera",
                    "turnos",
                    "agenda_bloqueos",
                    "horarios_atencion",
                    "profesional_institucion",
                    "profesional_especialidad",
                    "pacientes",
                    "secretarias",
                    "profesionales",
                    "usuarios",
                    "auditoria",
                    "especialidades",
                    "instituciones",
                    "obras_sociales"
            };
            for (String table : tables) {
                try {
                    jdbcTemplate.update("DELETE FROM " + table);
                    resetAutoIncrement(jdbcTemplate, table);
                } catch (Exception ex) {
                    log.info("No se pudo limpiar tabla {}: {}", table, ex.getMessage());
                }
            }
            enableFkChecks(jdbcTemplate);
            log.warn("Base limpia. Se crearán catálogos mínimos y el admin inicial por los seeders.");
        };
    }

    private void disableFkChecks(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        } catch (Exception ignored) {
            // MySQL/MariaDB only.
        }
        try {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        } catch (Exception ignored) {
            // H2 only.
        }
    }

    private void enableFkChecks(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (Exception ignored) {
            // H2 only.
        }
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        } catch (Exception ignored) {
            // MySQL/MariaDB only.
        }
    }

    private void resetAutoIncrement(JdbcTemplate jdbcTemplate, String table) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " AUTO_INCREMENT = 1");
        } catch (Exception ignored) {
            // MySQL/MariaDB only.
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH 1");
        } catch (Exception ignored) {
            // H2 only.
        }
    }
}
