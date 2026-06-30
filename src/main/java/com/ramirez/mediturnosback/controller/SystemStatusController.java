package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {
    private static final String STATUS_OK = "OK";
    private static final String STATUS_ERROR = "ERROR";
    private static final String STATUS_SIN_CONFIGURAR = "SIN_CONFIGURAR";

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;

    @Value("${app.frontend.reset-url:}")
    private String resetUrl;

    @Value("${app.frontend.verify-url:}")
    private String verifyUrl;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.upload.dir:}")
    private String uploadDir;

    public SystemStatusController(JdbcTemplate jdbcTemplate, CurrentUserService currentUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/diagnostico")
    public Map<String, Object> diagnostico() {
        currentUserService.requireAnyRole(RolUsuario.ADMIN);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("backend", STATUS_OK);
        status.put("db", checkDb());
        status.put("auth", STATUS_OK);
        status.put("uploads", statusConfiguracion(uploadDir));
        status.put("brevo", statusConfiguracion(brevoApiKey));
        status.put("resetUrl", statusConfiguracion(resetUrl));
        status.put("verifyUrl", statusConfiguracion(verifyUrl));
        status.put("version", "v15.4");
        return status;
    }

    private String statusConfiguracion(String value) {
        return value == null || value.isBlank() ? STATUS_SIN_CONFIGURAR : STATUS_OK;
    }

    private String checkDb() {
        try {
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
            return one != null && one == 1 ? STATUS_OK : STATUS_ERROR;
        } catch (Exception e) {
            return STATUS_ERROR;
        }
    }
}
