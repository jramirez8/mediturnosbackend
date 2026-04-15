package com.ramirez.mediturnosback.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "turno_adjuntos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurnoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    @JsonIgnore
    private Turno turno;

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 120)
    private String mimeType;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String contenidoBase64;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
