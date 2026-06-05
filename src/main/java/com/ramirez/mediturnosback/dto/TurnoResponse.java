package com.ramirez.mediturnosback.dto;

import com.ramirez.mediturnosback.model.EstadoTurno;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoResponse {
    private Long id;
    private LocalDateTime fechaHora;
    private LocalDateTime fechaHoraFin;
    private EstadoTurno estado;
    private String observaciones;
    private Long institucionId;
    private String institucionNombre;
    private String direccionAtencion;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteApellido;
    private String pacienteDni;
    private Long profesionalId;
    private Long profesionalInstitucionId;
    private String profesionalNombre;
    private String profesionalApellido;
    private Long especialidadId;
    private String especialidad;
    private String telefonoProfesional;
    private String documentacionNombreArchivo;
    private String documentacionMimeType;
    private String documentacionBase64;
    private Long documentacionId;
    private String documentacionUrl;
    private Long documentacionSizeBytes;
    private String motivoConsulta;
    private String enfermedadActual;
    private String antecedenteEnfermedadActual;
    private String antecedentesPersonales;
    private String antecedentesFamiliares;
    private String medicacionActual;
    private String alergias;
    private String habitos;
    private String hallazgosExamenFisico;
    private String conducta;
}
