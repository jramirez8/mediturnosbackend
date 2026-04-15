package com.ramirez.mediturnosback.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instituciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Institucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoInstitucion tipo = TipoInstitucion.OTRO;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(length = 40)
    private String telefono;

    @Column(length = 40)
    private String whatsapp;

    @Column(nullable = false)
    private Boolean activa = true;

    @Override
    public String toString() {
        return nombre + " - " + direccion;
    }
}
