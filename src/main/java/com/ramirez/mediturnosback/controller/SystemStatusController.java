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
        status.put("backend", "OK");
        status.put("db", checkDb());
        status.put("auth", "OK");
        status.put("uploads", uploadDir == null || uploadDir.isBlank() ? "SIN_CONFIGURAR" : "OK");
        status.put("brevo", brevoApiKey == null || brevoApiKey.isBlank() ? "SIN_CONFIGURAR" : "OK");
        status.put("resetUrl", resetUrl == null || resetUrl.isBlank() ? "SIN_CONFIGURAR" : "OK");
        status.put("verifyUrl", verifyUrl == null || verifyUrl.isBlank() ? "SIN_CONFIGURAR" : "OK");
        status.put("version", "v15.4");
        return status;
    }

    private String checkDb() {
        try {
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
            return one != null && one == 1 ? "OK" : "ERROR";
        } catch (Exception e) {
            return "ERROR";
        }
    }
}
