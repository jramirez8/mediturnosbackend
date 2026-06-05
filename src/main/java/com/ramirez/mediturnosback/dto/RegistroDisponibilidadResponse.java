package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroDisponibilidadResponse {
    private boolean disponible;
    private boolean dniRegistrado;
    private boolean emailRegistrado;
    private boolean telefonoRegistrado;
    private List<String> conflictos;
    private String message;
}
