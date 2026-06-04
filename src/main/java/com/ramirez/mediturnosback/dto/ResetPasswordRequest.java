package com.ramirez.mediturnosback.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {

    private String token;

    /** Campos oficiales. */
    private String password;
    private String confirmPassword;

    /** Alias tolerantes para frontends que usan otros nombres. */
    private String newPassword;
    private String confirmNewPassword;

    public String resolverPassword() {
        if (password != null && !password.isBlank()) return password;
        if (newPassword != null && !newPassword.isBlank()) return newPassword;
        return null;
    }

    public String resolverConfirmPassword() {
        if (confirmPassword != null && !confirmPassword.isBlank()) return confirmPassword;
        if (confirmNewPassword != null && !confirmNewPassword.isBlank()) return confirmNewPassword;
        return null;
    }
}
