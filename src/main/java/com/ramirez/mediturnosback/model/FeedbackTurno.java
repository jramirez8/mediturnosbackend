package com.ramirez.mediturnosback.model;

import com.ramirez.mediturnosback.util.AppClock;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "turnos_feedback", uniqueConstraints = @UniqueConstraint(columnNames = "turno_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackTurno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "turno_id", nullable = false, unique = true)
    private Turno turno;

    @Column(nullable = false)
    private Integer puntuacion;

    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now(AppClock.APP_ZONE);
}
