package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacienteRegistroResponse {
    private Long usuarioId;
    private Long pacienteId;
    private String mensaje;
    private boolean requiereVerificacionEmail;
}
