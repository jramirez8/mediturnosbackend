package com.ramirez.mediturnosback.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "paciente_documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    @JsonIgnore
    private Paciente paciente;

    @ManyToOne(optional = true)
    @JoinColumn(name = "turno_id")
    @JsonIgnore
    private Turno turno;

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 120)
    private String mimeType;

    @Column(length = 60)
    private String tipoDocumento;

    @Column(length = 700, nullable = false)
    private String storagePath;

    private Long originalSizeBytes;

    private Long storedSizeBytes;

    private Long subidoPorUsuarioId;

    @Column(length = 160)
    private String subidoPorEmail;

    @Column(length = 40)
    private String subidoPorRol;

    @Column(nullable = false)
    private Boolean archivado = false;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    private LocalDateTime archivadoEn;
}
