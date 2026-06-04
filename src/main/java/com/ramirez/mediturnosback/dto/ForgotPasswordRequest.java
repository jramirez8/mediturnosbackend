package com.ramirez.mediturnosback.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequest {

    /** Campo oficial del backend: email o DNI. */
    private String identificador;

    /** Alias usado por algunas pantallas mobile/web. */
    private String emailOrDni;

    /** Alias tolerantes. */
    private String email;
    private String dni;

    public String resolverIdentificador() {
        if (identificador != null && !identificador.isBlank()) return identificador.trim();
        if (emailOrDni != null && !emailOrDni.isBlank()) return emailOrDni.trim();
        if (email != null && !email.isBlank()) return email.trim();
        if (dni != null && !dni.isBlank()) return dni.trim();
        return null;
    }
}
