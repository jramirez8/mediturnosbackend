package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.AuditLogResponse;
import com.ramirez.mediturnosback.model.AuditLog;
import com.ramirez.mediturnosback.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void registrar(String accion, String entidad, Long entidadId, String actor, String detalle) {
        AuditLog log = new AuditLog();
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setEntidadId(entidadId);
        log.setActor(actor != null && !actor.isBlank() ? actor : "sistema");
        log.setDetalle(detalle);
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> ultimos() {
        return auditLogRepository.findTop100ByOrderByCreadoEnDesc().stream().map(this::map).toList();
    }

    private AuditLogResponse map(AuditLog l) {
        return new AuditLogResponse(l.getId(), l.getAccion(), l.getEntidad(), l.getEntidadId(), l.getActor(), l.getDetalle(), l.getCreadoEn());
    }
}
