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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
                       @Value("${app.auth.expose-reset-token:false}") boolean exposeResetToken) {
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
        paciente.setNumeroHistoriaClinica(generarNumeroHistoriaClinica());
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
                "Cuenta creada. Revisá tu correo para activar la cuenta.",
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

        if (debeUsarSegundoFactor(usuario)) {
            String codigo = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            usuario.setTwoFactorCode(codigo);
            usuario.setTwoFactorCodeExpiraEn(LocalDateTime.now().plusMinutes(10));
            usuarioRepository.save(usuario);
            boolean enviado = verificationDispatchService.enviarCodigoDosFactores(usuario, codigo);
            if (!enviado) {
                throw new IllegalStateException("No se pudo enviar el código de verificación por email.");
            }
            return new AuthLoginResponse(
                    usuario.getId(), pacienteId, profesionalId, usuario.getRol(), usuario.getEmail(), nombreCompleto,
                    Boolean.TRUE.equals(usuario.getEmailVerificado()),
                    "Te enviamos un código de verificación a tu correo.",
                    null, null, null, "Bearer", true, ocultarEmail(usuario.getEmail())
            );
        }

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
                "Bearer",
                false,
                null
        );
    }

    @Transactional
    public AuthLoginResponse verificarSegundoFactor(AuthTwoFactorVerifyRequest request) {
        if (request.getUsuarioId() == null) throw new IllegalArgumentException("Falta usuario para validar 2FA");
        if (request.getCodigo() == null || request.getCodigo().isBlank()) throw new IllegalArgumentException("Ingresá el código de verificación");
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (usuario.getTwoFactorCode() == null || usuario.getTwoFactorCodeExpiraEn() == null) {
            throw new IllegalArgumentException("No hay una verificación de segundo factor pendiente");
        }
        if (usuario.getTwoFactorCodeExpiraEn().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El código de verificación expiró");
        }
        if (!usuario.getTwoFactorCode().equals(request.getCodigo().trim())) {
            throw new IllegalArgumentException("Código de verificación inválido");
        }

        usuario.setTwoFactorCode(null);
        usuario.setTwoFactorCodeExpiraEn(null);
        usuarioRepository.save(usuario);

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
        return new AuthLoginResponse(usuario.getId(), pacienteId, profesionalId, usuario.getRol(), usuario.getEmail(), nombreCompleto,
                Boolean.TRUE.equals(usuario.getEmailVerificado()), "Inicio de sesión correcto", token, token, token, "Bearer", false, null);
    }

    private boolean debeUsarSegundoFactor(Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getTwoFactorEmailEnabled()) || usuario.getRol() == RolUsuario.ADMIN;
    }

    private String ocultarEmail(String email) {
        if (email == null || !email.contains("@")) return "tu email";
        String[] parts = email.split("@", 2);
        String left = parts[0];
        String maskedLeft = left.length() <= 2 ? left.charAt(0) + "***" : left.substring(0, 2) + "***";
        return maskedLeft + "@" + parts[1];
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

    public RegistroDisponibilidadResponse validarDisponibilidadRegistro(RegistroDisponibilidadRequest request) {
        String dni = request.getDni().trim();
        String email = request.getEmail().trim();
        String telefono = request.getTelefono().trim();

        boolean dniRegistrado = pacienteRepository.existsByDni(dni);
        boolean emailRegistrado = usuarioRepository.existsByEmailIgnoreCase(email);
        boolean telefonoRegistrado = pacienteRepository.existsByTelefono(telefono);

        List<String> conflictos = new ArrayList<>();
        if (dniRegistrado) conflictos.add("DNI");
        if (emailRegistrado) conflictos.add("email");
        if (telefonoRegistrado) conflictos.add("teléfono");

        boolean disponible = conflictos.isEmpty();
        return new RegistroDisponibilidadResponse(
                disponible,
                dniRegistrado,
                emailRegistrado,
                telefonoRegistrado,
                conflictos,
                disponible
                        ? "Los datos están disponibles para registrar una cuenta."
                        : "Ya existe una cuenta registrada con " + String.join(", ", conflictos) + "."
        );
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
                    if (!emailEnviado) {
                        throw new IllegalStateException("No se pudo enviar el correo de recuperación. Revisá la configuración de Brevo.");
                    }
                    return new PasswordRecoveryResponse(
                            "Te enviamos un correo con instrucciones para recuperar la contraseña.",
                            exposeResetToken ? usuario.getTokenRecuperacion() : null,
                            exposeResetToken ? verificationDispatchService.generarResetUrl(usuario.getTokenRecuperacion()) : null,
                            true
                    );
                })
                .orElseThrow(() -> new IllegalArgumentException("No encontramos una cuenta registrada con ese DNI o email."));
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
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese email");
        }
        if (pacienteRepository.existsByDni(request.getDni().trim())) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese DNI");
        }
        if (pacienteRepository.existsByTelefono(request.getTelefono().trim())) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese teléfono");
        }
    }

    private String generarNumeroHistoriaClinica() {
        long numero = pacienteRepository.count() + 1;
        String candidato;
        do {
            candidato = "HC-" + String.format("%06d", numero++);
        } while (pacienteRepository.existsByNumeroHistoriaClinica(candidato));
        return candidato;
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
