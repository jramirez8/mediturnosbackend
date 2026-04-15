package com.ramirez.mediturnosback.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "turno_id", nullable = false, unique = true)
    @JsonIgnore
    private Turno turno;

    @Column(nullable = false)
    private LocalDateTime fechaAtencion = LocalDateTime.now();

    @Lob
    private String motivoConsulta;
    @Lob
    private String enfermedadActual;
    @Lob
    private String antecedenteEnfermedadActual;
    @Lob
    private String antecedentesPersonales;
    @Lob
    private String antecedentesFamiliares;
    @Lob
    private String medicacionActual;
    @Lob
    private String alergias;
    @Lob
    private String habitos;
    @Lob
    private String hallazgosExamenFisico;
    @Lob
    private String conducta;
}
