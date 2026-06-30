package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.InstitucionRepository;
import com.ramirez.mediturnosback.repository.ObraSocialRepository;
import com.ramirez.mediturnosback.repository.PacienteRepository;
import com.ramirez.mediturnosback.repository.ProfesionalRepository;
import com.ramirez.mediturnosback.repository.ProfesionalInstitucionRepository;
import com.ramirez.mediturnosback.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String LOGIN_ERROR = "LOGIN_ERROR";
    private static final String TABLA_USUARIOS = "usuarios";
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final ObraSocialRepository obraSocialRepository;
    private final InstitucionRepository institucionRepository;
    private final ProfesionalRepository profesionalRepository;
    private final ProfesionalInstitucionRepository profesionalInstitucionRepository;
    private final VerificationDispatchService verificationDispatchService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String appBaseUrl;
    private final boolean exposeResetToken;

    public AuthService(UsuarioRepository usuarioRepository,
                       PacienteRepository pacienteRepository,
                       ObraSocialRepository obraSocialRepository,
                       InstitucionRepository institucionRepository,
                       ProfesionalRepository profesionalRepository,
                       ProfesionalInstitucionRepository profesionalInstitucionRepository,
                       VerificationDispatchService verificationDispatchService,
                       AuditService auditService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.base-url:http://127.0.0.1:8080}") String appBaseUrl,
                       @Value("${app.auth.expose-reset-token:false}") boolean exposeResetToken) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.obraSocialRepository = obraSocialRepository;
        this.institucionRepository = institucionRepository;
        this.profesionalRepository = profesionalRepository;
        this.profesionalInstitucionRepository = profesionalInstitucionRepository;
        this.verificationDispatchService = verificationDispatchService;
        this.auditService = auditService;
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
        usuario.setTokenVerificacion(generarCodigoSeisDigitos());
        usuario.setTokenVerificacionExpiraEn(LocalDateTime.now().plusMinutes(15));

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
        boolean enviado = verificationDispatchService.enviarCodigoValidacionEmail(guardado.getUsuario(), guardado, guardado.getUsuario().getTokenVerificacion());
        if (!enviado) {
            throw new IllegalStateException("No se pudo enviar el código de verificación. Revisá la configuración de Brevo.");
        }
        auditService.registrarSistema("REGISTRO_PACIENTE", "pacientes", guardado.getId(), "Registro pendiente de verificación para " + guardado.getUsuario().getEmail());
        return new PacienteRegistroResponse(
                guardado.getUsuario().getId(),
                guardado.getId(),
                "✅ Cuenta creada. Te enviamos un código de 6 dígitos para verificarla.",
                true
        );
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        String identificador = request.resolverIdentificador();
        if (identificador == null || identificador.isBlank()) {
            auditService.registrarSistema(LOGIN_ERROR, TABLA_USUARIOS, null, "Intento de login sin identificador");
            throw new IllegalArgumentException("Ingresá email o DNI");
        }

        Usuario usuario = usuarioRepository.findByEmailOrPacienteDni(identificador)
                .orElseThrow(() -> {
                    auditService.registrarSistema(LOGIN_ERROR, TABLA_USUARIOS, null, "Credenciales inválidas para " + identificador);
                    return new IllegalArgumentException("Credenciales inválidas");
                });

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            auditService.registrarSistema(LOGIN_ERROR, TABLA_USUARIOS, usuario.getId(), "Password inválido para " + usuario.getEmail());
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        if (Boolean.FALSE.equals(usuario.getEmailVerificado())) {
            auditService.registrarSistema(LOGIN_ERROR, TABLA_USUARIOS, usuario.getId(), "Cuenta sin verificar: " + usuario.getEmail());
            throw new IllegalArgumentException("Debés verificar tu correo antes de ingresar. Revisá el código que te enviamos al registrarte.");
        }
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            auditService.registrarSistema(LOGIN_ERROR, TABLA_USUARIOS, usuario.getId(), "Cuenta inactiva: " + usuario.getEmail());
            throw new IllegalArgumentException("Tu cuenta está inactiva");
        }

        Long pacienteId = usuario.getPaciente() != null ? usuario.getPaciente().getId() : null;
        Long profesionalId = usuario.getProfesional() != null ? usuario.getProfesional().getId() : null;
        Long profesionalInstitucionId = resolverProfesionalInstitucionId(usuario);
        String nombreCompleto = nombreCompleto(usuario);

        if (debeUsarSegundoFactor(usuario)) {
            String codigo = generarCodigoSeisDigitos();
            usuario.setTwoFactorCode(codigo);
            usuario.setTwoFactorCodeExpiraEn(LocalDateTime.now().plusMinutes(10));
            usuarioRepository.save(usuario);
            boolean enviado = verificationDispatchService.enviarCodigoDosFactores(usuario, codigo);
            if (!enviado) {
                throw new IllegalStateException("No se pudo enviar el código de verificación por email.");
            }
            return new AuthLoginResponse(
                    usuario.getId(), pacienteId, profesionalId, profesionalInstitucionId, usuario.getRol(), usuario.getEmail(), nombreCompleto,
                    Boolean.TRUE.equals(usuario.getEmailVerificado()),
                    "Te enviamos un código de verificación a tu correo.",
                    null, null, null, TOKEN_TYPE_BEARER, true, ocultarEmail(usuario.getEmail())
            );
        }

        String token = jwtService.generarToken(usuario);
        auditService.registrarSistema("LOGIN_OK", TABLA_USUARIOS, usuario.getId(), "Inicio de sesión correcto: " + usuario.getEmail());

        return new AuthLoginResponse(
                usuario.getId(),
                pacienteId,
                profesionalId,
                profesionalInstitucionId,
                usuario.getRol(),
                usuario.getEmail(),
                nombreCompleto,
                Boolean.TRUE.equals(usuario.getEmailVerificado()),
                "Inicio de sesión correcto",
                token,
                token,
                token,
                TOKEN_TYPE_BEARER,
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
        Long profesionalInstitucionId = resolverProfesionalInstitucionId(usuario);
        String nombreCompleto = nombreCompleto(usuario);
        String token = jwtService.generarToken(usuario);
        return new AuthLoginResponse(usuario.getId(), pacienteId, profesionalId, profesionalInstitucionId, usuario.getRol(), usuario.getEmail(), nombreCompleto,
                Boolean.TRUE.equals(usuario.getEmailVerificado()), "Inicio de sesión correcto", token, token, token, TOKEN_TYPE_BEARER, false, null);
    }

    private Long resolverProfesionalInstitucionId(Usuario usuario) {
        if (usuario == null || usuario.getProfesional() == null || usuario.getProfesional().getId() == null) return null;
        return profesionalInstitucionRepository.findByProfesionalIdAndActivoTrue(usuario.getProfesional().getId())
                .stream()
                .findFirst()
                .map(ProfesionalInstitucion::getId)
                .orElse(null);
    }

    private boolean debeUsarSegundoFactor(Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getTwoFactorEmailEnabled());
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


    @Transactional
    public String verificarCuentaConCodigo(VerifyAccountRequest request) {
        String identificador = request.resolverIdentificador();
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("Ingresá email o DNI");
        }
        if (request.getCodigo() == null || request.getCodigo().isBlank()) {
            throw new IllegalArgumentException("Ingresá el código de verificación");
        }
        Usuario usuario = usuarioRepository.findByEmailOrPacienteDni(identificador)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos una cuenta para verificar"));
        if (Boolean.TRUE.equals(usuario.getEmailVerificado()) && Boolean.TRUE.equals(usuario.getActivo())) {
            return "La cuenta ya estaba verificada. Ya podés iniciar sesión.";
        }
        if (usuario.getTokenVerificacion() == null || usuario.getTokenVerificacionExpiraEn() == null) {
            throw new IllegalArgumentException("No hay una verificación pendiente para esta cuenta");
        }
        if (usuario.getTokenVerificacionExpiraEn().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El código de verificación expiró. Pedí uno nuevo desde el registro o contactá a administración.");
        }
        if (!usuario.getTokenVerificacion().equals(request.getCodigo().trim())) {
            throw new IllegalArgumentException("Código de verificación inválido");
        }
        usuario.setEmailVerificado(true);
        usuario.setActivo(true);
        usuario.setTokenVerificacion(null);
        usuario.setTokenVerificacionExpiraEn(null);
        usuarioRepository.save(usuario);
        auditService.registrarSistema("CUENTA_VERIFICADA", TABLA_USUARIOS, usuario.getId(), "Cuenta verificada por código: " + usuario.getEmail());
        return "✅ Cuenta verificada correctamente. Ya podés iniciar sesión.";
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

        var usuarioOpt = usuarioRepository.findByEmailOrPacienteDni(identificador);
        if (usuarioOpt.isEmpty()) {
            auditService.registrarSistema("PASSWORD_RECOVERY_REQUEST", TABLA_USUARIOS, null, "Solicitud de recuperación para identificador no encontrado: " + identificador);
            return new PasswordRecoveryResponse(
                    "Solicitud enviada. Si los datos son correctos, te enviamos un código para restablecer tu clave.",
                    null,
                    null,
                    true
            );
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setTokenRecuperacion(generarCodigoSeisDigitos());
        usuario.setTokenRecuperacionExpiraEn(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);
        boolean emailEnviado = verificationDispatchService.enviarRecuperacionEmail(usuario, usuario.getTokenRecuperacion());
        if (!emailEnviado) {
            throw new IllegalStateException("No se pudo enviar el correo de recuperación. Revisá la configuración de Brevo.");
        }
        auditService.registrarSistema("PASSWORD_RECOVERY_REQUEST", TABLA_USUARIOS, usuario.getId(), "Código de recuperación enviado a " + usuario.getEmail());
        return new PasswordRecoveryResponse(
                "Solicitud enviada. Si los datos son correctos, te enviamos un código para restablecer tu clave.",
                exposeResetToken ? usuario.getTokenRecuperacion() : null,
                exposeResetToken ? verificationDispatchService.generarResetUrl(usuario.getTokenRecuperacion()) : null,
                true
        );
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
        auditService.registrarSistema("PASSWORD_RESET_OK", TABLA_USUARIOS, usuario.getId(), "Contraseña restablecida para " + usuario.getEmail());
        return "Contraseña actualizada correctamente";
    }

    private String generarCodigoSeisDigitos() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String nombreCompleto(Usuario usuario) {
        if (usuario.getPaciente() != null) {
            return usuario.getPaciente().getNombre() + " " + usuario.getPaciente().getApellido();
        }
        if (usuario.getProfesional() != null) {
            return usuario.getProfesional().getNombre() + " " + usuario.getProfesional().getApellido();
        }
        if (usuario.getSecretaria() != null) {
            return usuario.getSecretaria().getNombre() + " " + usuario.getSecretaria().getApellido();
        }
        return usuario.getEmail();
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
