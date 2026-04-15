package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalDto {
    private Long id;
    private Long profesionalInstitucionId;
    private Long institucionId;
    private Long especialidadId;
    private String nombre;
    private String apellido;
    private String matricula;
    private String especialidad;
    private String telefono;
    private String institucionNombre;
    private String direccionConsultorio;
    private String email;
    private String nombreCompleto;
}
