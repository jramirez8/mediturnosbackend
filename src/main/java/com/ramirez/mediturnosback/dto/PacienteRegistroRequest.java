package com.ramirez.mediturnosback.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ramirez.mediturnosback.model.TipoSangre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PacienteRegistroRequest {
    @NotBlank @Size(max = 100) private String nombre;
    @NotBlank @Size(max = 100) private String apellido;
    @NotBlank @Size(max = 20) private String dni;
    @NotBlank @Email @Size(max = 100) private String email;
    @NotBlank @Size(min = 8, max = 100) private String password;
    @NotBlank @Size(min = 8, max = 100) private String confirmPassword;
    @NotNull private Long obraSocialId;
    @NotNull private TipoSangre tipoSangre;
    @Size(max = 100) private String numeroCarnet;
    @Size(max = 50) private String numeroHistoriaClinica;
    @Size(max = 150) private String hospitalClinicaCabecera;
    @Size(max = 150) private String doctorCabecera;
    @NotNull @Past @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaNacimiento;
    @NotBlank @Size(max = 30) private String telefono;
}
