package com.ramirez.mediturnosback.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "pacientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String apellido;

    @NotBlank
    @Column(unique = true, nullable = false, length = 20)
    private String dni;

    @NotNull
    @Past
    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @NotBlank
    @Column(nullable = false, length = 30)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSangre tipoSangre;

    @Column(length = 100)
    private String numeroCarnet;

    @Column(nullable = false, unique = true, length = 50)
    private String numeroHistoriaClinica;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String fotoPerfilBase64;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String carnetObraSocialBase64;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "obra_social_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ObraSocial obraSocial;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institucion_cabecera_id")
    private Institucion institucionCabecera;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medico_cabecera_profesional_id")
    @JsonIgnoreProperties({"usuario", "sedes", "especialidades"})
    private Profesional medicoCabecera;


    @Column(nullable = false)
    private Boolean activo = true;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Transient
    public String getHospitalClinicaCabecera() {
        return institucionCabecera != null ? institucionCabecera.getNombre() : null;
    }

    @Transient
    public String getDoctorCabecera() {
        return medicoCabecera != null ? medicoCabecera.getNombreCompleto() : null;
    }
}
