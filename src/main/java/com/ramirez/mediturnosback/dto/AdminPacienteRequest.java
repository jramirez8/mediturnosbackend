package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.TipoSangre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminPacienteRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    @NotBlank
    private String dni;
    @NotNull
    private LocalDate fechaNacimiento;
    @NotBlank
    private String telefono;
    @NotNull
    private TipoSangre tipoSangre;
    @NotNull
    private Long obraSocialId;
    private String numeroCarnet;
    @NotBlank
    private String numeroHistoriaClinica;
    private Long institucionCabeceraId;
    private Long medicoCabeceraProfesionalId;
    private Boolean activo = true;
    private Boolean emailVerificado = true;
}
