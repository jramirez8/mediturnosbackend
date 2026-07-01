package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.*;
import com.ramirez.mediturnosback.util.AppClock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TABLA_USUARIOS = "usuarios";
    private static final String TABLA_PACIENTES = "pacientes";
    private static final String TABLA_PROFESIONALES = "profesionales";
    private static final String TABLA_SECRETARIAS = "secretarias";
    private static final String TABLA_INSTITUCIONES = "instituciones";
    private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado con id: ";
    private static final String INSTITUCION_NO_ENCONTRADA = "Institución no encontrada con id: ";
    private static final String ESPECIALIDAD_NO_ENCONTRADA = "Especialidad no encontrada con id: ";
    private static final String OBRA_SOCIAL_NO_ENCONTRADA = "Obra social no encontrada con id: ";
    private static final String PROFESIONAL_NO_ENCONTRADO = "Profesional no encontrado con id: ";

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final SecretariaRepository secretariaRepository;
    private final TurnoRepository turnoRepository;
    private final InstitucionRepository institucionRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ObraSocialRepository obraSocialRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final VerificationDispatchService verificationDispatchService;

    public AdminService(UsuarioRepository usuarioRepository,
                        PacienteRepository pacienteRepository,
                        ProfesionalRepository profesionalRepository,
                        SecretariaRepository secretariaRepository,
                        TurnoRepository turnoRepository,
                        InstitucionRepository institucionRepository,
                        EspecialidadRepository especialidadRepository,
                        ObraSocialRepository obraSocialRepository,
                        HorarioAtencionRepository horarioAtencionRepository,
                        PasswordEncoder passwordEncoder,
                        AuditService auditService,
                        VerificationDispatchService verificationDispatchService) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.secretariaRepository = secretariaRepository;
        this.turnoRepository = turnoRepository;
        this.institucionRepository = institucionRepository;
        this.especialidadRepository = especialidadRepository;
        this.obraSocialRepository = obraSocialRepository;
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.verificationDispatchService = verificationDispatchService;
    }

    public Map<String, Object> resumen() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put(TABLA_USUARIOS, usuarioRepository.count());
        out.put(TABLA_PACIENTES, pacienteRepository.count());
        out.put(TABLA_PROFESIONALES, profesionalRepository.count());
        out.put(TABLA_SECRETARIAS, secretariaRepository.count());
        out.put("turnos", turnoRepository.count());
        out.put(TABLA_INSTITUCIONES, institucionRepository.count());
        out.put("especialidades", especialidadRepository.count());
        out.put("obrasSociales", obraSocialRepository.count());
        out.put("horariosAtencion", horarioAtencionRepository.count());
        return out;
    }

    public List<RolUsuario> roles() {
        return List.of(RolUsuario.values());
    }

    public List<AdminUsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::toUsuarioResponse)
                .toList();
    }

    @Transactional
    public AdminUsuarioResponse crearUsuario(AdminUsuarioCreateRequest request) {
        validarAltaUsuarioGenerico(request.getRol());
        validarEmailUnico(request.getEmail(), null);
        validarPasswordInicial(request.getPassword());
        Usuario usuario = new Usuario();
        usuario.setEmail(normalizeEmail(request.getEmail()));
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(request.getRol());
        usuario.setActivo(valueOrDefault(request.getActivo(), true));
        usuario.setEmailVerificado(valueOrDefault(request.getEmailVerificado(), true));
        Usuario guardado = usuarioRepository.save(usuario);
        auditService.registrar("ADMIN_USUARIO_ALTA", TABLA_USUARIOS, guardado.getId(), null, "Usuario creado");
        return toUsuarioResponse(guardado);
    }

    @Transactional
    public AdminUsuarioResponse actualizarUsuario(Long id, AdminUsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USUARIO_NO_ENCONTRADO + id));
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            validarEmailUnico(request.getEmail(), usuario.getId());
            usuario.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            validarPasswordInicial(request.getPassword());
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRol() != null) {
            validarCambioRolSeguro(usuario, request.getRol());
            usuario.setRol(request.getRol());
        }
        if (request.getActivo() != null) usuario.setActivo(request.getActivo());
        if (request.getEmailVerificado() != null) usuario.setEmailVerificado(request.getEmailVerificado());
        Usuario guardado = usuarioRepository.save(usuario);
        auditService.registrar("ADMIN_USUARIO_EDICION", TABLA_USUARIOS, guardado.getId(), null, "Usuario actualizado");
        return toUsuarioResponse(guardado);
    }

    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USUARIO_NO_ENCONTRADO + id));
        usuario.setActivo(false);
        if (usuario.getPaciente() != null) usuario.getPaciente().setActivo(false);
        if (usuario.getProfesional() != null) usuario.getProfesional().setActivo(false);
        if (usuario.getSecretaria() != null) usuario.getSecretaria().setActiva(false);
        usuarioRepository.save(usuario);
        auditService.registrar("ADMIN_USUARIO_BAJA", TABLA_USUARIOS, id, null, "Usuario desactivado");
    }

    @Transactional
    public Map<String, Object> reenviarVerificacionUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USUARIO_NO_ENCONTRADO + id));
        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            return Map.of("ok", true, "message", "La cuenta ya está verificada.");
        }
        if (usuario.getPaciente() == null) {
            throw new IllegalArgumentException("Solo se puede reenviar verificación a cuentas de pacientes creadas con ficha clínica.");
        }
        String codigo = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        usuario.setTokenVerificacion(codigo);
        usuario.setTokenVerificacionExpiraEn(LocalDateTime.now(AppClock.APP_ZONE).plusMinutes(15));
        usuarioRepository.save(usuario);
        boolean enviado = verificationDispatchService.enviarCodigoValidacionEmail(usuario, usuario.getPaciente(), codigo);
        if (!enviado) throw new IllegalStateException("No se pudo enviar el correo de verificación. Revisá Brevo.");
        auditService.registrar("ADMIN_REENVIO_VERIFICACION", TABLA_USUARIOS, usuario.getId(), null, "Código de verificación reenviado a " + usuario.getEmail());
        return Map.of("ok", true, "message", "Código de verificación reenviado.");
    }

    public List<Institucion> listarInstituciones() { return institucionRepository.findAll(); }

    @Transactional
    public Institucion crearInstitucion(AdminInstitucionRequest request) {
        Institucion i = new Institucion();
        applyInstitucion(i, request);
        Institucion guardada = institucionRepository.save(i);
        auditService.registrar("ADMIN_INSTITUCION_ALTA", TABLA_INSTITUCIONES, guardada.getId(), null, "Institución creada");
        return guardada;
    }

    @Transactional
    public Institucion actualizarInstitucion(Long id, AdminInstitucionRequest request) {
        Institucion i = institucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(INSTITUCION_NO_ENCONTRADA + id));
        applyInstitucion(i, request);
        Institucion guardada = institucionRepository.save(i);
        auditService.registrar("ADMIN_INSTITUCION_EDICION", TABLA_INSTITUCIONES, guardada.getId(), null, "Institución actualizada");
        return guardada;
    }

    @Transactional
    public void desactivarInstitucion(Long id) {
        Institucion i = institucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(INSTITUCION_NO_ENCONTRADA + id));
        i.setActiva(false);
        institucionRepository.save(i);
        auditService.registrar("ADMIN_INSTITUCION_BAJA", TABLA_INSTITUCIONES, id, null, "Institución desactivada");
    }

    public List<Especialidad> listarEspecialidades() { return especialidadRepository.findAll(); }

    @Transactional
    public Especialidad crearEspecialidad(AdminEspecialidadRequest request) {
        Especialidad e = new Especialidad();
        e.setNombre(request.getNombre().trim());
        e.setActiva(valueOrDefault(request.getActiva(), true));
        return especialidadRepository.save(e);
    }

    @Transactional
    public Especialidad actualizarEspecialidad(Long id, AdminEspecialidadRequest request) {
        Especialidad e = especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ESPECIALIDAD_NO_ENCONTRADA + id));
        e.setNombre(request.getNombre().trim());
        e.setActiva(valueOrDefault(request.getActiva(), true));
        return especialidadRepository.save(e);
    }

    @Transactional
    public void desactivarEspecialidad(Long id) {
        Especialidad e = especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ESPECIALIDAD_NO_ENCONTRADA + id));
        e.setActiva(false);
        especialidadRepository.save(e);
    }

    public List<ObraSocial> listarObrasSociales() { return obraSocialRepository.findAll(); }

    @Transactional
    public ObraSocial crearObraSocial(AdminObraSocialRequest request) {
        ObraSocial o = new ObraSocial();
        o.setNombre(request.getNombre().trim());
        o.setCodigo(blankToNull(request.getCodigo()));
        o.setActiva(valueOrDefault(request.getActiva(), true));
        return obraSocialRepository.save(o);
    }

    @Transactional
    public ObraSocial actualizarObraSocial(Long id, AdminObraSocialRequest request) {
        ObraSocial o = obraSocialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(OBRA_SOCIAL_NO_ENCONTRADA + id));
        o.setNombre(request.getNombre().trim());
        o.setCodigo(blankToNull(request.getCodigo()));
        o.setActiva(valueOrDefault(request.getActiva(), true));
        return obraSocialRepository.save(o);
    }

    @Transactional
    public void desactivarObraSocial(Long id) {
        ObraSocial o = obraSocialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(OBRA_SOCIAL_NO_ENCONTRADA + id));
        o.setActiva(false);
        obraSocialRepository.save(o);
    }

    public List<AdminProfesionalResponse> listarProfesionales() {
        return profesionalRepository.findAll().stream().map(this::toProfesionalResponse).toList();
    }

    @Transactional
    public AdminProfesionalResponse crearProfesional(AdminProfesionalRequest request) {
        validarEmailUnico(request.getEmail(), null);
        validarDniUnicoProfesional(request.getDni(), null);
        validarMatriculaUnica(request.getMatricula(), null);
        validarPasswordInicial(request.getPassword());

        Usuario usuario = new Usuario();
        usuario.setEmail(normalizeEmail(request.getEmail()));
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(RolUsuario.PROFESSIONAL);
        usuario.setActivo(valueOrDefault(request.getActivo(), true));
        usuario.setEmailVerificado(valueOrDefault(request.getEmailVerificado(), true));

        Profesional profesional = new Profesional();
        profesional.setUsuario(usuario);
        applyProfesionalBase(profesional, request.getNombre(), request.getApellido(), request.getDni(), request.getMatricula(), request.getTelefono(), valueOrDefault(request.getActivo(), true));
        profesional.setEspecialidades(resolveEspecialidades(request.getEspecialidadIds()));
        profesional.setSedes(resolveInstitucionesAsSedes(profesional, request.getInstitucionIds()));

        Profesional guardado = profesionalRepository.save(profesional);
        auditService.registrar("ADMIN_PROFESIONAL_ALTA", TABLA_PROFESIONALES, guardado.getId(), null, "Profesional creado");
        return toProfesionalResponse(guardado);
    }

    @Transactional
    public AdminProfesionalResponse actualizarProfesional(Long id, AdminProfesionalUpdateRequest request) {
        Profesional profesional = profesionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROFESIONAL_NO_ENCONTRADO + id));
        Usuario usuario = profesional.getUsuario();

        actualizarUsuarioProfesional(usuario, request);
        actualizarDatosProfesional(profesional, request);

        Profesional guardado = profesionalRepository.save(profesional);
        auditService.registrar("ADMIN_PROFESIONAL_EDICION", TABLA_PROFESIONALES, guardado.getId(), null, "Profesional actualizado");
        return toProfesionalResponse(guardado);
    }

    private void actualizarUsuarioProfesional(Usuario usuario, AdminProfesionalUpdateRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            validarEmailUnico(request.getEmail(), usuario.getId());
            usuario.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            validarPasswordInicial(request.getPassword());
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }
        if (request.getEmailVerificado() != null) usuario.setEmailVerificado(request.getEmailVerificado());
    }

    private void actualizarDatosProfesional(Profesional profesional, AdminProfesionalUpdateRequest request) {
        if (request.getActivo() != null) {
            profesional.setActivo(request.getActivo());
        }
        if (request.getDni() != null && !request.getDni().isBlank()) {
            validarDniUnicoProfesional(request.getDni(), profesional.getId());
            profesional.setDni(request.getDni().trim());
        }
        if (request.getMatricula() != null && !request.getMatricula().isBlank()) {
            validarMatriculaUnica(request.getMatricula(), profesional.getId());
            profesional.setMatricula(request.getMatricula().trim());
        }
        if (request.getNombre() != null && !request.getNombre().isBlank()) profesional.setNombre(request.getNombre().trim());
        if (request.getApellido() != null && !request.getApellido().isBlank()) profesional.setApellido(request.getApellido().trim());
        if (request.getTelefono() != null) profesional.setTelefono(blankToNull(request.getTelefono()));
        if (request.getEspecialidadIds() != null && !request.getEspecialidadIds().isEmpty()) {
            profesional.getEspecialidades().clear();
            profesional.getEspecialidades().addAll(resolveEspecialidades(request.getEspecialidadIds()));
        }
        if (request.getInstitucionIds() != null && !request.getInstitucionIds().isEmpty()) {
            profesional.getSedes().clear();
            profesional.getSedes().addAll(resolveInstitucionesAsSedes(profesional, request.getInstitucionIds()));
        }
    }

    @Transactional
    public void desactivarProfesional(Long id) {
        Profesional p = profesionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROFESIONAL_NO_ENCONTRADO + id));
        p.setActivo(false);
        p.getUsuario().setActivo(false);
        profesionalRepository.save(p);
        auditService.registrar("ADMIN_PROFESIONAL_BAJA", TABLA_PROFESIONALES, id, null, "Profesional desactivado");
    }

    public List<AdminSecretariaResponse> listarSecretarias() {
        return secretariaRepository.findAll().stream().map(this::toSecretariaResponse).toList();
    }

    @Transactional
    public AdminSecretariaResponse crearSecretaria(AdminSecretariaRequest request) {
        validarEmailUnico(request.getEmail(), null);
        validarDniUnicoSecretaria(request.getDni(), null);
        validarPasswordInicial(request.getPassword());
        Institucion institucion = resolveInstitucion(request.getInstitucionId());

        Usuario usuario = new Usuario();
        usuario.setEmail(normalizeEmail(request.getEmail()));
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(RolUsuario.SECRETARY);
        usuario.setActivo(valueOrDefault(request.getActiva(), true));
        usuario.setEmailVerificado(valueOrDefault(request.getEmailVerificado(), true));

        Secretaria secretaria = new Secretaria();
        secretaria.setUsuario(usuario);
        secretaria.setNombre(request.getNombre().trim());
        secretaria.setApellido(request.getApellido().trim());
        secretaria.setDni(request.getDni().trim());
        secretaria.setTelefono(blankToNull(request.getTelefono()));
        secretaria.setInstitucion(institucion);
        secretaria.setActiva(valueOrDefault(request.getActiva(), true));

        Secretaria guardada = secretariaRepository.save(secretaria);
        auditService.registrar("ADMIN_SECRETARIA_ALTA", TABLA_SECRETARIAS, guardada.getId(), null, "Secretaría creada");
        return toSecretariaResponse(guardada);
    }

    @Transactional
    public AdminSecretariaResponse actualizarSecretaria(Long id, AdminSecretariaUpdateRequest request) {
        Secretaria secretaria = secretariaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Secretaria no encontrada con id: " + id));
        Usuario usuario = secretaria.getUsuario();
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            validarEmailUnico(request.getEmail(), usuario.getId());
            usuario.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            validarPasswordInicial(request.getPassword());
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEmailVerificado() != null) usuario.setEmailVerificado(request.getEmailVerificado());
        if (request.getActiva() != null) {
            usuario.setActivo(request.getActiva());
            secretaria.setActiva(request.getActiva());
        }
        if (request.getNombre() != null && !request.getNombre().isBlank()) secretaria.setNombre(request.getNombre().trim());
        if (request.getApellido() != null && !request.getApellido().isBlank()) secretaria.setApellido(request.getApellido().trim());
        if (request.getDni() != null && !request.getDni().isBlank()) {
            validarDniUnicoSecretaria(request.getDni(), secretaria.getId());
            secretaria.setDni(request.getDni().trim());
        }
        if (request.getTelefono() != null) secretaria.setTelefono(blankToNull(request.getTelefono()));
        if (request.getInstitucionId() != null) secretaria.setInstitucion(resolveInstitucion(request.getInstitucionId()));
        Secretaria guardada = secretariaRepository.save(secretaria);
        auditService.registrar("ADMIN_SECRETARIA_EDICION", TABLA_SECRETARIAS, guardada.getId(), null, "Secretaría actualizada");
        return toSecretariaResponse(guardada);
    }

    @Transactional
    public void desactivarSecretaria(Long id) {
        Secretaria s = secretariaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Secretaria no encontrada con id: " + id));
        s.setActiva(false);
        s.getUsuario().setActivo(false);
        secretariaRepository.save(s);
        auditService.registrar("ADMIN_SECRETARIA_BAJA", TABLA_SECRETARIAS, id, null, "Secretaría desactivada");
    }

    public List<AdminPacienteResponse> listarPacientes() {
        return pacienteRepository.findAll().stream().map(this::toPacienteResponse).toList();
    }

    @Transactional
    public AdminPacienteResponse crearPaciente(AdminPacienteRequest request) {
        validarEmailUnico(request.getEmail(), null);
        validarDniUnicoPaciente(request.getDni(), null);
        validarHistoriaClinicaUnica(request.getNumeroHistoriaClinica(), null);
        validarPasswordInicial(request.getPassword());
        ObraSocial obraSocial = resolveObraSocial(request.getObraSocialId());

        Usuario usuario = new Usuario();
        usuario.setEmail(normalizeEmail(request.getEmail()));
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(RolUsuario.PATIENT);
        usuario.setActivo(valueOrDefault(request.getActivo(), true));
        usuario.setEmailVerificado(valueOrDefault(request.getEmailVerificado(), true));

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);
        applyPacienteBase(paciente, request, obraSocial, valueOrDefault(request.getActivo(), true));
        Paciente guardado = pacienteRepository.save(paciente);
        auditService.registrar("ADMIN_PACIENTE_ALTA", TABLA_PACIENTES, guardado.getId(), null, "Paciente creado");
        return toPacienteResponse(guardado);
    }

    @Transactional
    public AdminPacienteResponse actualizarPaciente(Long id, AdminPacienteUpdateRequest request) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));
        Usuario usuario = paciente.getUsuario();

        actualizarUsuarioPaciente(usuario, paciente, request);
        actualizarDatosPaciente(paciente, request);

        Paciente guardado = pacienteRepository.save(paciente);
        auditService.registrar("ADMIN_PACIENTE_EDICION", TABLA_PACIENTES, guardado.getId(), null, "Paciente actualizado");
        return toPacienteResponse(guardado);
    }

    private void actualizarUsuarioPaciente(Usuario usuario, Paciente paciente, AdminPacienteUpdateRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            validarEmailUnico(request.getEmail(), usuario.getId());
            usuario.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            validarPasswordInicial(request.getPassword());
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEmailVerificado() != null) usuario.setEmailVerificado(request.getEmailVerificado());
        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
            paciente.setActivo(request.getActivo());
        }
    }

    private void actualizarDatosPaciente(Paciente paciente, AdminPacienteUpdateRequest request) {
        if (request.getNombre() != null && !request.getNombre().isBlank()) paciente.setNombre(request.getNombre().trim());
        if (request.getApellido() != null && !request.getApellido().isBlank()) paciente.setApellido(request.getApellido().trim());
        if (request.getDni() != null && !request.getDni().isBlank()) {
            validarDniUnicoPaciente(request.getDni(), paciente.getId());
            paciente.setDni(request.getDni().trim());
        }
        if (request.getFechaNacimiento() != null) {
            paciente.setFechaNacimiento(request.getFechaNacimiento());
        }
        if (request.getTelefono() != null) paciente.setTelefono(request.getTelefono().trim());
        if (request.getTipoSangre() != null) paciente.setTipoSangre(request.getTipoSangre());
        if (request.getObraSocialId() != null && request.getObraSocialId() > 0) {
            paciente.setObraSocial(resolveObraSocial(request.getObraSocialId()));
        }
        paciente.setNumeroCarnet(blankToNull(request.getNumeroCarnet()));
        if (request.getNumeroHistoriaClinica() != null && !request.getNumeroHistoriaClinica().isBlank()) {
            validarHistoriaClinicaUnica(request.getNumeroHistoriaClinica(), paciente.getId());
            paciente.setNumeroHistoriaClinica(request.getNumeroHistoriaClinica().trim());
        }
        aplicarInstitucionCabecera(paciente, request.getInstitucionCabeceraId());
        aplicarMedicoCabecera(paciente, request.getMedicoCabeceraProfesionalId());
    }

    @Transactional
    public void desactivarPaciente(Long id) {
        Paciente p = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));
        p.setActivo(false);
        p.getUsuario().setActivo(false);
        pacienteRepository.save(p);
        auditService.registrar("ADMIN_PACIENTE_BAJA", TABLA_PACIENTES, id, null, "Paciente desactivado");
    }

    private void applyInstitucion(Institucion i, AdminInstitucionRequest request) {
        i.setNombre(request.getNombre().trim());
        i.setTipo(request.getTipo() != null ? request.getTipo() : TipoInstitucion.OTRO);
        i.setDireccion(request.getDireccion().trim());
        i.setTelefono(blankToNull(request.getTelefono()));
        i.setWhatsapp(blankToNull(request.getWhatsapp()));
        i.setActiva(valueOrDefault(request.getActiva(), true));
    }

    private void applyProfesionalBase(Profesional profesional, String nombre, String apellido, String dni, String matricula, String telefono, boolean activo) {
        profesional.setNombre(nombre.trim());
        profesional.setApellido(apellido.trim());
        profesional.setDni(blankToNull(dni));
        profesional.setMatricula(matricula.trim());
        profesional.setTelefono(blankToNull(telefono));
        profesional.setActivo(activo);
    }

    private void applyPacienteBase(Paciente paciente, AdminPacienteRequest request, ObraSocial obraSocial, boolean activo) {
        paciente.setNombre(request.getNombre().trim());
        paciente.setApellido(request.getApellido().trim());
        paciente.setDni(request.getDni().trim());
        paciente.setFechaNacimiento(request.getFechaNacimiento());
        paciente.setTelefono(request.getTelefono().trim());
        paciente.setTipoSangre(request.getTipoSangre());
        paciente.setObraSocial(obraSocial);
        paciente.setNumeroCarnet(blankToNull(request.getNumeroCarnet()));
        paciente.setNumeroHistoriaClinica(request.getNumeroHistoriaClinica().trim());
        aplicarInstitucionCabecera(paciente, request.getInstitucionCabeceraId());
        aplicarMedicoCabecera(paciente, request.getMedicoCabeceraProfesionalId());
        paciente.setActivo(activo);
    }

    private void aplicarInstitucionCabecera(Paciente paciente, Long institucionCabeceraId) {
        if (institucionCabeceraId == null) {
            return;
        }
        paciente.setInstitucionCabecera(institucionCabeceraId > 0 ? resolveInstitucion(institucionCabeceraId) : null);
    }

    private void aplicarMedicoCabecera(Paciente paciente, Long medicoCabeceraProfesionalId) {
        if (medicoCabeceraProfesionalId == null) {
            return;
        }
        paciente.setMedicoCabecera(medicoCabeceraProfesionalId > 0 ? resolveProfesional(medicoCabeceraProfesionalId) : null);
    }

    private Set<Especialidad> resolveEspecialidades(List<Long> ids) {
        return ids.stream().map(id -> especialidadRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(ESPECIALIDAD_NO_ENCONTRADA + id)))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Set<ProfesionalInstitucion> resolveInstitucionesAsSedes(Profesional profesional, List<Long> ids) {
        return ids.stream().map(this::resolveInstitucion)
                .map(i -> {
                    ProfesionalInstitucion pi = new ProfesionalInstitucion();
                    pi.setProfesional(profesional);
                    pi.setInstitucion(i);
                    pi.setActivo(true);
                    return pi;
                }).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Institucion resolveInstitucion(Long id) {
        return institucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(INSTITUCION_NO_ENCONTRADA + id));
    }

    private ObraSocial resolveObraSocial(Long id) {
        return obraSocialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(OBRA_SOCIAL_NO_ENCONTRADA + id));
    }

    private Profesional resolveProfesional(Long id) {
        return profesionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROFESIONAL_NO_ENCONTRADO + id));
    }

    private AdminUsuarioResponse toUsuarioResponse(Usuario usuario) {
        Long pacienteId = usuario.getPaciente() != null ? usuario.getPaciente().getId() : null;
        Long profesionalId = usuario.getProfesional() != null ? usuario.getProfesional().getId() : null;
        Long secretariaId = usuario.getSecretaria() != null ? usuario.getSecretaria().getId() : null;
        String nombreMostrar = nombreMostrar(usuario);
        String dni = dniUsuario(usuario);
        return new AdminUsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getRol(), usuario.getActivo(), usuario.getEmailVerificado(), pacienteId, profesionalId, secretariaId, nombreMostrar, dni);
    }

    private String nombreMostrar(Usuario usuario) {
        if (usuario.getPaciente() != null) {
            return usuario.getPaciente().getApellido() + ", " + usuario.getPaciente().getNombre();
        }
        if (usuario.getProfesional() != null) {
            return usuario.getProfesional().getApellido() + ", " + usuario.getProfesional().getNombre();
        }
        if (usuario.getSecretaria() != null) {
            return usuario.getSecretaria().getApellido() + ", " + usuario.getSecretaria().getNombre();
        }
        return usuario.getEmail();
    }

    private String dniUsuario(Usuario usuario) {
        if (usuario.getPaciente() != null) {
            return usuario.getPaciente().getDni();
        }
        if (usuario.getProfesional() != null) {
            return usuario.getProfesional().getDni();
        }
        if (usuario.getSecretaria() != null) {
            return usuario.getSecretaria().getDni();
        }
        return null;
    }

    private AdminProfesionalResponse toProfesionalResponse(Profesional p) {
        return new AdminProfesionalResponse(
                p.getId(),
                p.getUsuario() != null ? p.getUsuario().getId() : null,
                p.getUsuario() != null ? p.getUsuario().getEmail() : null,
                p.getNombre(),
                p.getApellido(),
                p.getDni(),
                p.getMatricula(),
                p.getTelefono(),
                p.getActivo(),
                p.getEspecialidades().stream().map(Especialidad::getNombre).sorted().toList(),
                p.getSedes().stream().map(s -> s.getInstitucion().getNombre()).sorted().toList()
        );
    }

    private AdminSecretariaResponse toSecretariaResponse(Secretaria s) {
        return new AdminSecretariaResponse(
                s.getId(),
                s.getUsuario() != null ? s.getUsuario().getId() : null,
                s.getUsuario() != null ? s.getUsuario().getEmail() : null,
                s.getNombre(),
                s.getApellido(),
                s.getDni(),
                s.getTelefono(),
                s.getActiva(),
                s.getInstitucion() != null ? s.getInstitucion().getNombre() : null
        );
    }

    private AdminPacienteResponse toPacienteResponse(Paciente p) {
        return new AdminPacienteResponse(
                p.getId(),
                p.getUsuario() != null ? p.getUsuario().getId() : null,
                p.getUsuario() != null ? p.getUsuario().getEmail() : null,
                p.getNombre(),
                p.getApellido(),
                p.getDni(),
                p.getFechaNacimiento(),
                p.getTelefono(),
                p.getTipoSangre(),
                p.getObraSocial() != null ? p.getObraSocial().getNombre() : null,
                p.getNumeroCarnet(),
                p.getNumeroHistoriaClinica(),
                p.getInstitucionCabecera() != null ? p.getInstitucionCabecera().getNombre() : null,
                p.getMedicoCabecera() != null ? p.getMedicoCabecera().getNombreCompleto() : null,
                p.getActivo()
        );
    }

    private void validarEmailUnico(String email, Long usuarioIdActual) {
        usuarioRepository.findByEmailIgnoreCase(normalizeEmail(email)).ifPresent(u -> {
            if (usuarioIdActual == null || !u.getId().equals(usuarioIdActual)) {
                throw new IllegalArgumentException("Ya existe un usuario con ese email");
            }
        });
    }

    private void validarDniUnicoProfesional(String dni, Long profesionalIdActual) {
        if (dni == null || dni.isBlank()) return;
        profesionalRepository.findAll().stream().filter(p -> dni.trim().equalsIgnoreCase(p.getDni()))
                .findFirst().ifPresent(p -> {
                    if (profesionalIdActual == null || !p.getId().equals(profesionalIdActual)) {
                        throw new IllegalArgumentException("Ya existe un profesional con ese DNI");
                    }
                });
    }

    private void validarMatriculaUnica(String matricula, Long profesionalIdActual) {
        profesionalRepository.findAll().stream().filter(p -> matricula.trim().equalsIgnoreCase(p.getMatricula()))
                .findFirst().ifPresent(p -> {
                    if (profesionalIdActual == null || !p.getId().equals(profesionalIdActual)) {
                        throw new IllegalArgumentException("Ya existe un profesional con esa matrícula");
                    }
                });
    }

    private void validarDniUnicoSecretaria(String dni, Long secretariaIdActual) {
        secretariaRepository.findAll().stream().filter(s -> dni.trim().equalsIgnoreCase(s.getDni()))
                .findFirst().ifPresent(s -> {
                    if (secretariaIdActual == null || !s.getId().equals(secretariaIdActual)) {
                        throw new IllegalArgumentException("Ya existe una secretaria con ese DNI");
                    }
                });
    }

    private void validarDniUnicoPaciente(String dni, Long pacienteIdActual) {
        pacienteRepository.findAll().stream().filter(p -> dni.trim().equalsIgnoreCase(p.getDni()))
                .findFirst().ifPresent(p -> {
                    if (pacienteIdActual == null || !p.getId().equals(pacienteIdActual)) {
                        throw new IllegalArgumentException("Ya existe un paciente con ese DNI");
                    }
                });
    }

    private void validarHistoriaClinicaUnica(String hc, Long pacienteIdActual) {
        if (hc == null || hc.isBlank()) return;
        pacienteRepository.findAll().stream().filter(p -> hc.trim().equalsIgnoreCase(p.getNumeroHistoriaClinica()))
                .findFirst().ifPresent(p -> {
                    if (pacienteIdActual == null || !p.getId().equals(pacienteIdActual)) {
                        throw new IllegalArgumentException("Ya existe un paciente con ese número de historia clínica");
                    }
                });
    }

    private void validarAltaUsuarioGenerico(RolUsuario rol) {
        if (rol == null) throw new IllegalArgumentException("Seleccioná un rol");
        if (rol == RolUsuario.PROFESSIONAL) {
            throw new IllegalArgumentException("Para crear un médico usá Admin > Personal > Médicos. Ahí se crea el usuario y queda vinculado al profesional, especialidad y sede.");
        }
        if (rol == RolUsuario.SECRETARY) {
            throw new IllegalArgumentException("Para crear una secretaría usá Admin > Personal > Secretaría. Ahí se crea el usuario y queda vinculado a una institución.");
        }
        if (rol == RolUsuario.PATIENT) {
            throw new IllegalArgumentException("Para crear un paciente usá Admin > Personal > Pacientes o el registro público. Así queda vinculado a su ficha clínica.");
        }
    }

    private void validarCambioRolSeguro(Usuario usuario, RolUsuario nuevoRol) {
        if (nuevoRol == null) return;
        if (nuevoRol == RolUsuario.PROFESSIONAL && usuario.getProfesional() == null) {
            throw new IllegalArgumentException("Este usuario no está vinculado a un médico. Crealo desde Admin > Personal > Médicos o editá un médico existente.");
        }
        if (nuevoRol == RolUsuario.SECRETARY && usuario.getSecretaria() == null) {
            throw new IllegalArgumentException("Este usuario no está vinculado a una secretaría. Crealo desde Admin > Personal > Secretaría.");
        }
        if (nuevoRol == RolUsuario.PATIENT && usuario.getPaciente() == null) {
            throw new IllegalArgumentException("Este usuario no está vinculado a un paciente. Crealo desde Admin > Personal > Pacientes o desde registro.");
        }
    }

    private void validarPasswordInicial(String password) {
        if (password == null || password.isBlank() || password.trim().length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean valueOrDefault(Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }
}
