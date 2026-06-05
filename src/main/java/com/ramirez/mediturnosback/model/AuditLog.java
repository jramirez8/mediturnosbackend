package com.ramirez.mediturnosback.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String accion;

    @Column(length = 80)
    private String entidad;

    private Long entidadId;

    @Column(length = 140)
    private String actor;

    @Column(length = 1200)
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
