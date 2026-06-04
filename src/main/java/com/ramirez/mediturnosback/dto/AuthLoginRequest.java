package com.ramirez.mediturnosback.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLoginRequest {

    /**
     * Campo oficial del backend. Puede ser email o DNI.
     */
    private String identificador;

    /**
     * Alias tolerante para frontends que mandan { email, password }.
     */
    private String email;

    /**
     * Alias tolerante por si algún cliente manda DNI separado.
     */
    private String dni;

    @NotBlank
    private String password;

    public String resolverIdentificador() {
        if (identificador != null && !identificador.isBlank()) return identificador.trim();
        if (email != null && !email.isBlank()) return email.trim();
        if (dni != null && !dni.isBlank()) return dni.trim();
        return null;
    }
}
