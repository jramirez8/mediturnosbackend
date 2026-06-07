package com.ramirez.mediturnosback.security;

import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parsearClaimsDesdeAuthorization(authorization);
            RolUsuario rol = claims.get("rol") != null ? RolUsuario.valueOf(String.valueOf(claims.get("rol"))) : null;
            AuthenticatedUser user = new AuthenticatedUser(
                    jwtService.leerLongClaimPublic(claims, "usuarioId"),
                    jwtService.leerLongClaimPublic(claims, "pacienteId"),
                    jwtService.leerLongClaimPublic(claims, "profesionalId"),
                    rol,
                    claims.getSubject()
            );
            List<SimpleGrantedAuthority> authorities = rol != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))
                    : List.of();
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Token inválido o vencido\",\"error\":\"Token inválido o vencido\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
