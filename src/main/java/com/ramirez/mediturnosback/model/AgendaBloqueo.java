package com.ramirez.mediturnosback.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agenda_bloqueos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgendaBloqueo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "profesional_institucion_id", nullable = false)
    private ProfesionalInstitucion profesionalInstitucion;
    @Column(nullable = false)
    private LocalDateTime fechaDesde;
    @Column(nullable = false)
    private LocalDateTime fechaHasta;
    @Column(length = 255)
    private String motivo;
}
