package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.InstitucionRepository;
import com.ramirez.mediturnosback.repository.ObraSocialRepository;
import com.ramirez.mediturnosback.repository.PacienteRepository;
import com.ramirez.mediturnosback.repository.ProfesionalRepository;
import com.ramirez.mediturnosback.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final ObraSocialRepository obraSocialRepository;
    private final InstitucionRepository institucionRepository;
    private final ProfesionalRepository profesionalRepository;
    private final VerificationDispatchService verificationDispatchService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String appBaseUrl;
    private final boolean exposeResetToken;

    public AuthService(UsuarioRepository usuarioRepository,
                       PacienteRepository pacienteRepository,
                       ObraSocialRepository obraSocialRepository,
                       InstitucionRepository institucionRepository,
                       ProfesionalRepository profesionalRepository,
                       VerificationDispatchService verificationDispatchService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.base-url:http://127.0.0.1:8080}") String appBaseUrl,
                       @Value("${app.auth.expose-reset-token:true}") boolean exposeResetToken) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.obraSocialRepository = obraSocialRepository;
        this.institucionRepository = institucionRepository;
        this.profesionalRepository = profesionalRepository;
        this.verificationDispatchService = verificationDispatchService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appBaseUrl = appBaseUrl;
        this.exposeResetToken = exposeResetToken;
    }

    @Transactional
    public PacienteRegistroResponse registrarPaciente(PacienteRegistroRequest request) {
        validarPasswords(request.getPassword(), request.getConfirmPassword());
        validarDatosUnicosRegistro(request);

        ObraSocial obraSocial = obraSocialRepository.findById(request.getObraSocialId())
                .orElseThrow(() -> new ResourceNotFoundException("Obra social no encontrada con id: " + request.getObraSocialId()));

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail().trim().toLowerCase());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(RolUsuario.PATIENT);
        usuario.setEmailVerificado(false);
        usuario.setActivo(false);
        usuario.setTokenVerificacion(UUID.randomUUID().toString());
        usuario.setTokenVerificacionExpiraEn(LocalDateTime.now().plusDays(2));

        Paciente paciente = new Paciente();
        paciente.setNombre(request.getNombre().trim());
        paciente.setApellido(request.getApellido().trim());
        paciente.setDni(request.getDni().trim());
        paciente.setFechaNacimiento(request.getFechaNacimiento());
        paciente.setTelefono(request.getTelefono().trim());
        paciente.setTipoSangre(request.getTipoSangre());
        paciente.setNumeroCarnet(normalizarOpcional(request.getNumeroCarnet()));
        paciente.setNumeroHistoriaClinica(request.getNumeroHistoriaClinica().trim());
        paciente.setInstitucionCabecera(resolverInstitucionPorNombre(request.getHospitalClinicaCabecera()));
        paciente.setMedicoCabecera(resolverProfesionalPorNombre(request.getDoctorCabecera()));
        paciente.setObraSocial(obraSocial);
        paciente.setUsuario(usuario);

        Paciente guardado = pacienteRepository.save(paciente);
        String verificationUrl = appBaseUrl + "/api/auth/verificar-email?token=" + guardado.getUsuario().getTokenVerificacion();
        verificationDispatchService.enviarValidacionEmail(guardado.getUsuario(), guardado, verificationUrl);
        verificationDispatchService.enviarValidacionWhatsapp(guardado, guardado.getUsuario().getTokenVerificacion());
        return new PacienteRegistroResponse(
                guardado.getUsuario().getId(),
                guardado.getId(),
                "Cuenta creada. Revisá el correo o los logs del backend para ver el link de validación.",
                true
        );
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        String identificador = request.resolverIdentificador();
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("Ingresá email o DNI");
        }

        Usuario usuario = usuarioRepository.findByEmailOrPacienteDni(identificador)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        if (Boolean.FALSE.equals(usuario.getEmailVerificado())) {
            throw new IllegalArgumentException("Debés verificar tu correo antes de ingresar");
        }
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException("Tu cuenta está inactiva");
        }

        Long pacienteId = usuario.getPaciente() != null ? usuario.getPaciente().getId() : null;
        Long profesionalId = usuario.getProfesional() != null ? usuario.getProfesional().getId() : null;
        String nombreCompleto = usuario.getPaciente() != null
                ? usuario.getPaciente().getNombre() + " " + usuario.getPaciente().getApellido()
                : usuario.getProfesional() != null
                ? usuario.getProfesional().getNombre() + " " + usuario.getProfesional().getApellido()
                : usuario.getSecretaria() != null
                ? usuario.getSecretaria().getNombre() + " " + usuario.getSecretaria().getApellido()
                : usuario.getEmail();

        String token = jwtService.generarToken(usuario);

        return new AuthLoginResponse(
                usuario.getId(),
                pacienteId,
                profesionalId,
                usuario.getRol(),
                usuario.getEmail(),
                nombreCompleto,
                Boolean.TRUE.equals(usuario.getEmailVerificado()),
                "Inicio de sesión correcto",
                token,
                token,
                token,
                "Bearer"
        );
    }

    @Transactional
    public String verificarEmail(String token) {
        Usuario usuario = usuarioRepository.findByTokenVerificacion(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificación inválido"));
        if (usuario.getTokenVerificacionExpiraEn() == null || usuario.getTokenVerificacionExpiraEn().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token de verificación expiró");
        }
        usuario.setEmailVerificado(true);
        usuario.setActivo(true);
        usuario.setTokenVerificacion(null);
        usuario.setTokenVerificacionExpiraEn(null);
        usuarioRepository.save(usuario);
        return "Cuenta verificada correctamente";
    }

    @Transactional
    public PasswordRecoveryResponse solicitarRecuperacionPassword(ForgotPasswordRequest request) {
        String identificador = request.resolverIdentificador();
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("Ingresá email o DNI");
        }

        return usuarioRepository.findByEmailOrPacienteDni(identificador)
                .map(usuario -> {
                    usuario.setTokenRecuperacion(UUID.randomUUID().toString());
                    usuario.setTokenRecuperacionExpiraEn(LocalDateTime.now().plusHours(2));
                    usuarioRepository.save(usuario);
                    boolean emailEnviado = verificationDispatchService.enviarRecuperacionEmail(usuario, usuario.getTokenRecuperacion());
                    String resetUrl = verificationDispatchService.generarResetUrl(usuario.getTokenRecuperacion());
                    return new PasswordRecoveryResponse(
                            emailEnviado
                                    ? "Te enviamos un correo con instrucciones para recuperar la contraseña."
                                    : "Modo demo: se generó un token de recuperación. También quedó en los logs del backend.",
                            exposeResetToken ? usuario.getTokenRecuperacion() : null,
                            exposeResetToken ? resetUrl : null,
                            emailEnviado
                    );
                })
                .orElseGet(() -> new PasswordRecoveryResponse(
                        "Si la cuenta existe, se generó una instrucción de recuperación.",
                        null,
                        null,
                        false
                ));
    }

    @Transactional
    public String restablecerPassword(ResetPasswordRequest request) {
        String password = request.resolverPassword();
        String confirmPassword = request.resolverConfirmPassword();
        validarPasswords(password, confirmPassword);

        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new IllegalArgumentException("El token de recuperación es obligatorio");
        }

        Usuario usuario = usuarioRepository.findByTokenRecuperacion(request.getToken().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Token de recuperación inválido"));
        if (usuario.getTokenRecuperacionExpiraEn() == null || usuario.getTokenRecuperacionExpiraEn().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token de recuperación expiró");
        }
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setTokenRecuperacion(null);
        usuario.setTokenRecuperacionExpiraEn(null);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);
        return "Contraseña actualizada correctamente";
    }

    private void validarPasswords(String password, String confirmPassword) {
        if (password == null || confirmPassword == null || password.isBlank() || confirmPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña y su confirmación son obligatorias");
        }
        if (!password.equals(confirmPassword)) throw new IllegalArgumentException("Las contraseñas no coinciden");
        if (password.length() < 8) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
    }

    private void validarDatosUnicosRegistro(PacienteRegistroRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }
        if (pacienteRepository.existsByDni(request.getDni().trim())) {
            throw new IllegalArgumentException("Ya existe un paciente con ese DNI");
        }
        if (pacienteRepository.existsByNumeroHistoriaClinica(request.getNumeroHistoriaClinica().trim())) {
            throw new IllegalArgumentException("Ya existe un paciente con ese número de historia clínica");
        }
    }

    private Institucion resolverInstitucionPorNombre(String valor) {
        String nombre = normalizarOpcional(valor);
        if (nombre == null) return null;
        return institucionRepository.findByNombreIgnoreCase(nombre).orElse(null);
    }

    private Profesional resolverProfesionalPorNombre(String valor) {
        String nombre = normalizarOpcional(valor);
        if (nombre == null) return null;
        String normalizado = nombre.toLowerCase();
        return profesionalRepository.findAll().stream()
                .filter(p -> p.getNombreCompleto().equalsIgnoreCase(nombre)
                        || ((p.getNombre() + " " + p.getApellido()).equalsIgnoreCase(nombre))
                        || ((p.getApellido() + ", " + p.getNombre()).equalsIgnoreCase(nombre))
                        || p.getApellido().toLowerCase().contains(normalizado)
                        || p.getNombre().toLowerCase().contains(normalizado))
                .findFirst()
                .orElse(null);
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) return null;
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
