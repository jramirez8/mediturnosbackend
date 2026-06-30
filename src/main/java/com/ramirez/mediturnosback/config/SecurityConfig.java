package com.ramirez.mediturnosback.config;

import com.ramirez.mediturnosback.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_PROFESSIONAL = "PROFESSIONAL";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/ping",
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/profesionales", "/api/profesionales/especialidades").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/instituciones", "/api/instituciones/**", "/api/obras-sociales", "/api/obras-sociales/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/turnos/disponibilidad").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(ROLE_ADMIN)
                        .requestMatchers("/api/secretaria/**").hasAnyRole("SECRETARY", ROLE_ADMIN)
                        .requestMatchers("/api/agenda/**").hasAnyRole(ROLE_PROFESSIONAL, ROLE_ADMIN)
                        .requestMatchers("/api/profesionales/me", "/api/profesionales/me/**", "/api/profesionales/agenda/**", "/api/profesionales/proximo-turno/**").hasAnyRole(ROLE_PROFESSIONAL, ROLE_ADMIN)
                        .requestMatchers("/api/profesionales/historial-paciente").hasAnyRole(ROLE_PROFESSIONAL, ROLE_ADMIN)
                        .requestMatchers("/api/lista-espera/pendientes").hasAnyRole("SECRETARY", ROLE_ADMIN)
                        .requestMatchers("/api/lista-espera/**").authenticated()
                        .requestMatchers("/api/pacientes/**").authenticated()
                        .requestMatchers("/api/turnos/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
