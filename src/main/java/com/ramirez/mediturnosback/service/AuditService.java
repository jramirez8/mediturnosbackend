package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.AuditLogResponse;
import com.ramirez.mediturnosback.model.AuditLog;
import com.ramirez.mediturnosback.repository.AuditLogRepository;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public AuditService(AuditLogRepository auditLogRepository, CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    public void registrar(String accion, String entidad, Long entidadId, String actor, String detalle) {
        Optional<AuthenticatedUser> current = currentUserService.optional();
        AuditLog log = new AuditLog();
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setEntidadId(entidadId);

        if (current.isPresent()) {
            AuthenticatedUser user = current.get();
            log.setActor(user.actorLabel());
            log.setActorUsuarioId(user.usuarioId());
            log.setActorRol(user.rol() != null ? user.rol().name() : null);
            log.setActorEmail(user.email());
        } else {
            log.setActor(actor != null && !actor.isBlank() ? actor : "sistema");
            log.setActorRol("SYSTEM");
        }

        log.setDetalle(detalle);
        auditLogRepository.save(log);
    }

    public void registrarSistema(String accion, String entidad, Long entidadId, String detalle) {
        AuditLog log = new AuditLog();
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setEntidadId(entidadId);
        log.setActor("sistema");
        log.setActorRol("SYSTEM");
        log.setDetalle(detalle);
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> ultimos() {
        return auditLogRepository.findTop100ByOrderByCreadoEnDesc().stream().map(this::map).toList();
    }

    private AuditLogResponse map(AuditLog l) {
        return new AuditLogResponse(
                l.getId(),
                l.getAccion(),
                l.getEntidad(),
                l.getEntidadId(),
                l.getActor(),
                l.getActorUsuarioId(),
                l.getActorRol(),
                l.getActorEmail(),
                l.getDetalle(),
                l.getCreadoEn()
        );
    }
}
