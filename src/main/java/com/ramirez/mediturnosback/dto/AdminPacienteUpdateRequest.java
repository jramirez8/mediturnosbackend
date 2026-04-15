package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.TipoSangre;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminPacienteUpdateRequest {
    @Email
    private String email;
    private String password;
    private String nombre;
    private String apellido;
    private String dni;
    private LocalDate fechaNacimiento;
    private String telefono;
    private TipoSangre tipoSangre;
    private Long obraSocialId;
    private String numeroCarnet;
    private String numeroHistoriaClinica;
    private Long institucionCabeceraId;
    private Long medicoCabeceraProfesionalId;
    private Boolean activo;
    private Boolean emailVerificado;
}
