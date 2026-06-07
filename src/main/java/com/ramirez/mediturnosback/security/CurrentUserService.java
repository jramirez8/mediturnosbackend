package com.ramirez.mediturnosback.security;

import com.ramirez.mediturnosback.model.RolUsuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class CurrentUserService {

    public Optional<AuthenticatedUser> optional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public AuthenticatedUser requireUser() {
        return optional().orElseThrow(() -> new AccessDeniedException("Necesitás iniciar sesión"));
    }

    public AuthenticatedUser requireAnyRole(RolUsuario... roles) {
        AuthenticatedUser user = requireUser();
        if (roles == null || roles.length == 0) {
            return user;
        }
        boolean ok = Arrays.stream(roles).anyMatch(role -> role == user.rol());
        if (!ok) {
            throw new AccessDeniedException("No tenés permisos para esta operación");
        }
        return user;
    }

    public boolean hasAnyRole(RolUsuario... roles) {
        return optional().map(user -> Arrays.stream(roles).anyMatch(role -> role == user.rol())).orElse(false);
    }

    public String actorLabelOrSystem() {
        return optional().map(AuthenticatedUser::actorLabel).orElse("sistema");
    }
}
