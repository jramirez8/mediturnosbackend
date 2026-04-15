package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.TipoSangre;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class AdminPacienteResponse {
    private Long id;
    private Long usuarioId;
    private String email;
    private String nombre;
    private String apellido;
    private String dni;
    private LocalDate fechaNacimiento;
    private String telefono;
    private TipoSangre tipoSangre;
    private String obraSocial;
    private String numeroCarnet;
    private String numeroHistoriaClinica;
    private String institucionCabecera;
    private String medicoCabecera;
    private Boolean activo;
}
