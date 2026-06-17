package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.model.Usuario;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService service = new JwtService("clave-super-segura-de-pruebas-1234567890", 3600);

    @Test
    void generaYLeeClaimsDelUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(15L);
        usuario.setEmail("medico@mediturnos.net.ar");
        usuario.setRol(RolUsuario.PROFESSIONAL);
        Paciente paciente = new Paciente();
        paciente.setId(77L);
        usuario.setPaciente(paciente);

        String token = service.generarToken(usuario);
        Claims claims = service.parsearClaimsDesdeAuthorization("Bearer " + token);

        assertThat(claims.getSubject()).isEqualTo("medico@mediturnos.net.ar");
        assertThat(service.leerLongClaimPublic(claims, "usuarioId")).isEqualTo(15L);
        assertThat(service.extraerPacienteIdDesdeAuthorization(token)).isEqualTo(77L);
        assertThat(service.extraerProfesionalIdDesdeAuthorization(token)).isNull();
        assertThat(claims.get("rol")).isEqualTo("PROFESSIONAL");
    }

    @Test
    void aceptaBearerSinImportarMayusculas() {
        Usuario usuario = usuario(2L);
        String token = service.generarToken(usuario);

        assertThat(service.extraerUsuarioIdDesdeAuthorization("bEaReR " + token)).isEqualTo(2L);
    }

    @Test
    void rechazaSecretosCortos() {
        assertThatThrownBy(() -> new JwtService("corta", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void rechazaAuthorizationVacio() {
        assertThatThrownBy(() -> service.parsearClaimsDesdeAuthorization(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authorization");
    }

    @Test
    void rechazaTokenVacioDespuesDeBearer() {
        assertThatThrownBy(() -> service.parsearClaimsDesdeAuthorization("Bearer   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail("usuario" + id + "@x.com");
        usuario.setRol(RolUsuario.ADMIN);
        return usuario;
    }
}
