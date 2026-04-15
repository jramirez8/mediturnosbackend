package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLoginRequest {

    @NotBlank
    private String identificador;

    @NotBlank
    private String password;
}
