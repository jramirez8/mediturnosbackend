package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroDisponibilidadRequest {
    @NotBlank @Size(max = 20) private String dni;
    @NotBlank @Email @Size(max = 100) private String email;
    @NotBlank @Size(max = 30) private String telefono;
}
