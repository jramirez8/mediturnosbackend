package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteRegistroResponse register(@Valid @RequestBody PacienteRegistroRequest request) {
        return authService.registrarPaciente(request);
    }

    @PostMapping("/login")
    public AuthLoginResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register/check")
    public RegistroDisponibilidadResponse checkRegistro(@Valid @RequestBody RegistroDisponibilidadRequest request) {
        return authService.validarDisponibilidadRegistro(request);
    }

    @GetMapping("/verificar-email")
    public SimpleMessageResponse verificarEmail(@RequestParam String token) {
        return new SimpleMessageResponse(authService.verificarEmail(token));
    }

    @PostMapping("/forgot-password")
    public PasswordRecoveryResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return authService.solicitarRecuperacionPassword(request);
    }

    @PostMapping("/reset-password")
    public SimpleMessageResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        return new SimpleMessageResponse(authService.restablecerPassword(request));
    }
}
