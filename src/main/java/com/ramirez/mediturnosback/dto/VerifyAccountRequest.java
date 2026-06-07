package com.ramirez.mediturnosback.dto;

import lombok.Data;

@Data
public class VerifyAccountRequest {
    private String identificador;
    private String email;
    private String dni;
    private String codigo;

    public String resolverIdentificador() {
        if (identificador != null && !identificador.isBlank()) return identificador.trim();
        if (email != null && !email.isBlank()) return email.trim();
        if (dni != null && !dni.isBlank()) return dni.trim();
        return null;
    }
}
