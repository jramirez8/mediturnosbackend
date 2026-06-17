package com.ramirez.mediturnosback.security;

import com.ramirez.mediturnosback.model.RolUsuario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    @Test
    void identificaCadaRol() {
        assertThat(new AuthenticatedUser(1L, 2L, null, RolUsuario.PATIENT, "p@x.com").isPatient()).isTrue();
        assertThat(new AuthenticatedUser(1L, null, 3L, RolUsuario.PROFESSIONAL, "m@x.com").isProfessional()).isTrue();
        assertThat(new AuthenticatedUser(1L, null, null, RolUsuario.SECRETARY, "s@x.com").isSecretary()).isTrue();
        assertThat(new AuthenticatedUser(1L, null, null, RolUsuario.ADMIN, "a@x.com").isAdmin()).isTrue();
    }

    @Test
    void actorLabelIncluyeEmailYRol() {
        AuthenticatedUser user = new AuthenticatedUser(7L, null, null, RolUsuario.ADMIN, "admin@mediturnos.net.ar");

        assertThat(user.actorLabel()).isEqualTo("admin@mediturnos.net.ar · ADMIN");
    }

    @Test
    void actorLabelUsaIdCuandoNoHayEmail() {
        AuthenticatedUser user = new AuthenticatedUser(9L, null, null, null, " ");

        assertThat(user.actorLabel()).isEqualTo("usuario#9");
    }
}
