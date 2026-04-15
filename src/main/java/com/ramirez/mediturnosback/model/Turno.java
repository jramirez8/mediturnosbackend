package com.ramirez.mediturnosback.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "turnos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTurno estado;

    @Lob
    @Column(name = "observaciones_paciente")
    private String observacionesPaciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    @JsonIgnoreProperties({"usuario", "fotoPerfilBase64", "carnetObraSocialBase64"})
    private Paciente paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    @JsonIgnoreProperties({"usuario", "sedes", "especialidades"})
    private Profesional profesional;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesional_institucion_id", nullable = false)
    private ProfesionalInstitucion profesionalInstitucion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @OneToOne(mappedBy = "turno", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Consulta consulta;

    @OneToMany(mappedBy = "turno", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TurnoAdjunto> adjuntos = new ArrayList<>();
}
