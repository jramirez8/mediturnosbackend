package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.TipoSangre;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacientePerfilResponse {
    private Long usuarioId;
    private Long pacienteId;
    private String nombre;
    private String apellido;
    private String dni;
    private LocalDate fechaNacimiento;
    private TipoSangre tipoSangre;
    private String email;
    private String telefono;
    private Long obraSocialId;
    private String obraSocialNombre;
    private String numeroCarnet;
    private String numeroHistoriaClinica;
    private Long institucionCabeceraId;
    private String hospitalClinicaCabecera;
    private Long medicoCabeceraProfesionalId;
    private String doctorCabecera;
    private Boolean emailVerificado;
    private String fotoPerfilBase64;
    private String carnetObraSocialBase64;
}
