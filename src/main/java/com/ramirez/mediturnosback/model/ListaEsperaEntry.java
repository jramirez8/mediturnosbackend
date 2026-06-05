package com.ramirez.mediturnosback.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lista_espera")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListaEsperaEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesional_institucion_id", nullable = false)
    private ProfesionalInstitucion profesionalInstitucion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    private LocalDate fechaPreferidaDesde;
    private LocalDate fechaPreferidaHasta;

    @Column(length = 700)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoListaEspera estado = EstadoListaEspera.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    private LocalDateTime notificadoEn;
}
