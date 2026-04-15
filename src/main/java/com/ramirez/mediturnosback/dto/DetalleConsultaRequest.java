package com.ramirez.mediturnosback.dto;

import lombok.Data;

@Data
public class DetalleConsultaRequest {
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
