package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret:mediturnos-demo-secret-key-cambiar-en-produccion-2026-uaDE}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        String normalizedSecret = secret == null ? "" : secret;
        if (normalizedSecret.length() < 32) {
            normalizedSecret = (normalizedSecret + "-mediturnos-demo-secret-key-cambiar").substring(0, 32);
        }
        this.key = Keys.hmacShaKeyFor(normalizedSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generarToken(Usuario usuario) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("usuarioId", usuario.getId())
                .claim("rol", usuario.getRol() != null ? usuario.getRol().name() : null)
                .claim("pacienteId", usuario.getPaciente() != null ? usuario.getPaciente().getId() : null)
                .claim("profesionalId", usuario.getProfesional() != null ? usuario.getProfesional().getId() : null)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }
}
