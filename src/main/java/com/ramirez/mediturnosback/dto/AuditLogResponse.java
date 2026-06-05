package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String accion;
    private String entidad;
    private Long entidadId;
    private String actor;
    private String detalle;
    private LocalDateTime creadoEn;
}
