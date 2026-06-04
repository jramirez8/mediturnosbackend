package com.ramirez.mediturnosback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordRecoveryResponse {
    private String mensaje;
    private String resetToken;
    private String resetUrl;
    private boolean emailEnviado;
}
