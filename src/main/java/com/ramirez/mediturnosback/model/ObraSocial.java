package com.ramirez.mediturnosback.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "obras_sociales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ObraSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Size(max = 20)
    @Column(unique = true, length = 20)
    private String codigo;

    @Column(nullable = false)
    private Boolean activa = true;
}
