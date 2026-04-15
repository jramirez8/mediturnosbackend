package com.ramirez.mediturnosback.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profesional_institucion", uniqueConstraints = @UniqueConstraint(columnNames = {"profesional_id", "institucion_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalInstitucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    @JsonIgnore
    private Profesional profesional;

    @ManyToOne(optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;

    @Column(length = 40)
    private String telefonoEnSede;

    @Column(length = 40)
    private String whatsappEnSede;

    @Column(nullable = false)
    private Boolean activo = true;
}
