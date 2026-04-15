package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PacientePerfilUpdateRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String telefono;
    @NotNull
    private Long obraSocialId;
    private String numeroCarnet;
    private String hospitalClinicaCabecera;
    private String doctorCabecera;
    private String fotoPerfilBase64;
    private String carnetObraSocialBase64;
}
