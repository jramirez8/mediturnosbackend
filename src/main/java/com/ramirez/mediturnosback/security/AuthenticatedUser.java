package com.ramirez.mediturnosback.security;

import com.ramirez.mediturnosback.model.RolUsuario;

public record AuthenticatedUser(
        Long usuarioId,
        Long pacienteId,
        Long profesionalId,
        RolUsuario rol,
        String email
) {
    public boolean isAdmin() {
        return rol == RolUsuario.ADMIN;
    }

    public boolean isPatient() {
        return rol == RolUsuario.PATIENT;
    }

    public boolean isProfessional() {
        return rol == RolUsuario.PROFESSIONAL;
    }

    public boolean isSecretary() {
        return rol == RolUsuario.SECRETARY;
    }

    public String actorLabel() {
        String base = email != null && !email.isBlank() ? email : "usuario#" + usuarioId;
        return rol != null ? base + " · " + rol.name() : base;
    }
}
