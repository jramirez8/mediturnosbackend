package com.ramirez.mediturnosback.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "horarios_atencion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioAtencion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "profesional_institucion_id", nullable = false)
    private ProfesionalInstitucion profesionalInstitucion;
    @ManyToOne(optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;
    @Column(nullable = false, length = 20)
    private String diaSemana;
    @Column(nullable = false)
    private LocalTime horaDesde;
    @Column(nullable = false)
    private LocalTime horaHasta;
    @Column(nullable = false)
    private Integer duracionTurnoMin = 30;
    @Column(nullable = false)
    private Boolean activo = true;
}
