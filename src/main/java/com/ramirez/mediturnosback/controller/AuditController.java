package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.AuditLogResponse;
import com.ramirez.mediturnosback.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/auditoria")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditLogResponse> ultimos() {
        return auditService.ultimos();
    }
}
