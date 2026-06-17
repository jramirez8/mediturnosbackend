package com.ramirez.mediturnosback.security;

import com.ramirez.mediturnosback.model.RolUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserServiceTest {

    private final CurrentUserService service = new CurrentUserService();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void optionalVacioSinAutenticacion() {
        assertThat(service.optional()).isEmpty();
    }

    @Test
    void optionalVacioSiPrincipalNoEsAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario", "clave", java.util.List.of())
        );

        assertThat(service.optional()).isEmpty();
    }

    @Test
    void devuelveUsuarioAutenticado() {
        AuthenticatedUser user = new AuthenticatedUser(1L, 2L, null, RolUsuario.PATIENT, "paciente@x.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, java.util.List.of())
        );

        assertThat(service.requireUser()).isEqualTo(user);
        assertThat(service.actorLabelOrSystem()).contains("paciente@x.com");
    }

    @Test
    void requireUserFallaSinSesion() {
        assertThatThrownBy(service::requireUser)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("iniciar sesión");
    }

    @Test
    void requireAnyRoleAceptaRolPermitido() {
        AuthenticatedUser user = autenticar(RolUsuario.SECRETARY);

        assertThat(service.requireAnyRole(RolUsuario.SECRETARY, RolUsuario.ADMIN)).isEqualTo(user);
        assertThat(service.hasAnyRole(RolUsuario.SECRETARY)).isTrue();
    }

    @Test
    void requireAnyRoleRechazaRolNoPermitido() {
        autenticar(RolUsuario.PATIENT);

        assertThatThrownBy(() -> service.requireAnyRole(RolUsuario.ADMIN))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permisos");
        assertThat(service.hasAnyRole(RolUsuario.ADMIN)).isFalse();
    }

    @Test
    void requireAnyRoleSinListaAceptaUsuario() {
        AuthenticatedUser user = autenticar(RolUsuario.ADMIN);

        assertThat(service.requireAnyRole()).isEqualTo(user);
    }

    private AuthenticatedUser autenticar(RolUsuario rol) {
        AuthenticatedUser user = new AuthenticatedUser(11L, 22L, 33L, rol, "user@x.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, java.util.List.of())
        );
        return user;
    }
}
