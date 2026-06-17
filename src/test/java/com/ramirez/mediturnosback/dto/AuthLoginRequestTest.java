package com.ramirez.mediturnosback.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthLoginRequestTest {

    @Test
    void usaIdentificadorConPrioridadYLoRecorta() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setIdentificador("  12345678  ");
        request.setEmail("otro@correo.com");
        request.setDni("99999999");

        assertThat(request.resolverIdentificador()).isEqualTo("12345678");
    }

    @Test
    void usaEmailCuandoNoHayIdentificador() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setEmail("  paciente@mediturnos.net.ar  ");

        assertThat(request.resolverIdentificador()).isEqualTo("paciente@mediturnos.net.ar");
    }

    @Test
    void usaDniComoUltimoAlias() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setDni(" 40111222 ");

        assertThat(request.resolverIdentificador()).isEqualTo("40111222");
    }

    @Test
    void devuelveNullCuandoTodosLosCamposEstanVacios() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setIdentificador(" ");
        request.setEmail("");
        request.setDni(null);

        assertThat(request.resolverIdentificador()).isNull();
    }
}
