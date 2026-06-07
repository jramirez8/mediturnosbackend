package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.*;
import com.ramirez.mediturnosback.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final SecretariaRepository secretariaRepository;
    private final TurnoRepository turnoRepository;
    private final InstitucionRepository institucionRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ObraSocialRepository obraSocialRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminService(UsuarioRepository usuarioRepository,
                        PacienteRepository pacienteRepository,
                        ProfesionalRepository profesionalRepository,
                        SecretariaRepository secretariaRepository,
                        TurnoRepository turnoRepository,
                        InstitucionRepository institucionRepository,
                        EspecialidadRepository especialidadRepository,
                        ObraSocialRepository obraSocialRepository,
                        PasswordEncoder passwordEncoder,
                        AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.secretariaRepository = secretariaRepository;
        this.turnoRepository = turnoRepository;
        this.institucionRepository = institucionRepository;
        this.especialidadRepository = especialidadRepository;
        this.obraSocialRepository = obraSocialRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public Map<String, Object> resumen() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("usuarios", usuarioRepository.count());
        out.put("pacientes", pacienteRepository.count());
        out.put("profesionales", profesionalRepository.count());
        out.put("secretarias", secretariaRepository.count());
        out.put("turnos", turnoRepository.count());
        out.put("instituciones", institucionRepository.count());
        out.put("especialidades", especialidadRepository.count());
        out.put("obrasSociales", obraSocialRepository.count());
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
        validarEmailUnico(request.getEmail(), null);
        Usuario usuario = new Usuario();
        usuario.setEmail(normalizeEmail(request.getEmail()));
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(request.getRol());
        usuario.setActivo(valueOrDefault(request.getActivo(), true));
        usuario.setEmailVerificado(valueOrDefault(request.getEmailVerificado(), true));
        Usuario guardado = usuarioRepository.save(usuario);
        auditService.registrar("ADMIN_USUARIO_ALTA", "usuarios", guardado.getId(), null, "Usuario creado");
        return toUsuarioResponse(guardado);
    }

    @Transactional
    public AdminUsuarioResponse actualizarUsuario(Long id, AdminUsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            validarEmailUnico(request.getEmail(), usuario.getId());
            usuario.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRol() != null) usuario.setRol(request.getRol());
        if (request.getActivo() != null) usuario.setActivo(request.getActivo());
        if (request.getEmailVerificado() != null) usuario.setEmailVerificado(request.getEmailVerificado());
        Usuario guardado = usuarioRepository.save(usuario);
        auditService.registrar("ADMIN_USUARIO_EDICION", "usuarios", guardado.getId(), null, "Usuario actualizado");
        return toUsuarioResponse(guardado);
    }

    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        usuario.setActivo(false);
        if (usuario.getPaciente() != null) usuario.getPaciente().setActivo(false);
        if (usuario.getProfesional() != null) usuario.getProfesional().setActivo(false);
        if (usuario.getSecretaria() != null) usuario.getSecretaria().setActiva(false);
        usuarioRepository.save(usuario);
        auditService.registrar("ADMIN_USUARIO_BAJA", "usuarios", id, null, "Usuario desactivado");
    }

    public List<Institucion> listarInstituciones() { return institucionRepository.findAll(); }

    @Transactional
    public Institucion crearInstitucion(AdminInstitucionRequest request) {
        Institucion i = new Institucion();
        applyInstitucion(i, request);
        Institucion guardada = institucionRepository.save(i);
        auditService.registrar("ADMIN_INSTITUCION_ALTA", "instituciones", guardada.getId(), null, "Institución creada");
        return guardada;
    }

    @Transactional
    public Institucion actualizarInstitucion(Long id, AdminInstitucionRequest request) {
        Institucion i = institucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institución no encontrada con id: " + id));
        applyInstitucion(i, request);
        Institucion guardada = institucionRepository.save(i);
        auditService.registrar("ADMIN_INSTITUCION_EDICION", "instituciones", guardada.getId(), null, "Institución actualizada");
        return guardada;
    }

    @Transactional
    public void desactivarInstitucion(Long id) {
        Institucion i = institucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institución no encontrada con id: " + id));
        i.setActiva(false);
        institucionRepository.save(i);
        auditService.registrar("ADMIN_INSTITUCION_BAJA", "instituciones", id, null, "Institución desactivada");
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
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + id));
        e.setNombre(request.getNombre().trim());
        e.setActiva(valueOrDefault(request.getActiva(), true));
        return especialidadRepository.save(e);
    }

    @Transactional
    public void desactivarEspecialidad(Long id) {
        Especialidad e = especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + id));
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
                .orElseThrow(() -> new ResourceNotFoundException("Obra social no encontrada con id: " + id));
        o.setNombre(request.getNombre().trim());
        o.setCodigo(blankToNull(request.getCodigo()));
        o.setActiva(valueOrDefault(request.getActiva(), true));
        return obraSocialRepository.save(o);
    }

    @Transactional
    public void desactivarObraSocial(Long id) {
        ObraSocial o = obraSocialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Obra social no encontrada con id: " + id));
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
        auditService.registrar("ADMIN_PROFESIONAL_ALTA", "profesionales", guardado.getId(), null, "Profesional creado");
        return toProfesionalResponse(guardado);
    }

    @Transactional
    public AdminProfesionalResponse actualizarProfesional(Long id, AdminProfesionalUpdateRequest request) {
        Profesional profesional = profesionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + id));
        Usuario usuario = profesional.getUsuario();

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            validarEmailUnico(request.getEmail(), usuario.getId());
            usuario.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
            profesional.setActivo(request.getActivo());
        }
        if (request.getEmailVerificado() != null) usuario.setEmailVerificado(request.getEmailVerificado());

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
        Profesional guardado = profesionalRepository.save(profesional);
        auditService.registrar("ADMIN_PROFESIONAL_EDICION", "profesionales", guardado.getId(), null, "Profesional actualizado");
        return toProfesionalResponse(guardado);
    }

    @Transactional
    public void desactivarProfesional(Long id) {
        Profesional p = profesionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + id));
        p.setActivo(false);
        p.getUsuario().setActivo(false);
        profesionalRepository.save(p);
        auditService.registrar("ADMIN_PROFESIONAL_BAJA", "profesionales", id, null, "Profesional desactivado");
    }

    public List<AdminSecretariaResponse> listarSecretarias() {
        return secretariaRepository.findAll().stream().map(this::toSecretariaResponse).toList();
    }

    @Transactional
    public AdminSecretariaResponse crearSecretaria(AdminSecretariaRequest request) {
        validarEmailUnico(request.getEmail(), null);
        validarDniUnicoSecretaria(request.getDni(), null);
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
        auditService.registrar("ADMIN_SECRETARIA_ALTA", "secretarias", guardada.getId(), null, "Secretaría creada");
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
        auditService.registrar("ADMIN_SECRETARIA_EDICION", "secretarias", guardada.getId(), null, "Secretaría actualizada");
        return toSecretariaResponse(guardada);
    }

    @Transactional
    public void desactivarSecretaria(Long id) {
        Secretaria s = secretariaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Secretaria no encontrada con id: " + id));
        s.setActiva(false);
        s.getUsuario().setActivo(false);
        secretariaRepository.save(s);
        auditService.registrar("ADMIN_SECRETARIA_BAJA", "secretarias", id, null, "Secretaría desactivada");
    }

    public List<AdminPacienteResponse> listarPacientes() {
        return pacienteRepository.findAll().stream().map(this::toPacienteResponse).toList();
    }

    @Transactional
    public AdminPacienteResponse crearPaciente(AdminPacienteRequest request) {
        validarEmailUnico(request.getEmail(), null);
        validarDniUnicoPaciente(request.getDni(), null);
        validarHistoriaClinicaUnica(request.getNumeroHistoriaClinica(), null);
        ObraSocial obraSocial = resolveObraSocial(request.getObraSocialId());

        Usuario usuario = new Usuario();
        usuario.setEmail(normalizeEmail(request.getEmail()));
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(RolUsuario.PATIENT);
        usuario.setActivo(valueOrDefault(request.getActivo(), true));
        usuario.setEmailVerificado(valueOrDefault(request.getEmailVerificado(), true));

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);
        applyPacienteBase(paciente, request.getNombre(), request.getApellido(), request.getDni(), request.getFechaNacimiento(), request.getTelefono(), request.getTipoSangre(), obraSocial, request.getNumeroCarnet(), request.getNumeroHistoriaClinica(), request.getInstitucionCabeceraId(), request.getMedicoCabeceraProfesionalId(), valueOrDefault(request.getActivo(), true));
        Paciente guardado = pacienteRepository.save(paciente);
        auditService.registrar("ADMIN_PACIENTE_ALTA", "pacientes", guardado.getId(), null, "Paciente creado");
        return toPacienteResponse(guardado);
    }

    @Transactional
    public AdminPacienteResponse actualizarPaciente(Long id, AdminPacienteUpdateRequest request) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));
        Usuario usuario = paciente.getUsuario();

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            validarEmailUnico(request.getEmail(), usuario.getId());
            usuario.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEmailVerificado() != null) usuario.setEmailVerificado(request.getEmailVerificado());
        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
            paciente.setActivo(request.getActivo());
        }

        // Campos de Paciente
        if (request.getNombre() != null && !request.getNombre().isBlank()) paciente.setNombre(request.getNombre().trim());
        if (request.getApellido() != null && !request.getApellido().isBlank()) paciente.setApellido(request.getApellido().trim());
        if (request.getDni() != null && !request.getDni().isBlank()) {
            validarDniUnicoPaciente(request.getDni(), paciente.getId());
            paciente.setDni(request.getDni().trim());
        }
        
        // Manejo seguro de Fecha
        if (request.getFechaNacimiento() != null) {
            paciente.setFechaNacimiento(request.getFechaNacimiento());
        }

        if (request.getTelefono() != null) paciente.setTelefono(request.getTelefono().trim());
        if (request.getTipoSangre() != null) paciente.setTipoSangre(request.getTipoSangre());
        
        // Manejo seguro de IDs (solo si son > 0)
        if (request.getObraSocialId() != null && request.getObraSocialId() > 0) {
            paciente.setObraSocial(resolveObraSocial(request.getObraSocialId()));
        }
        
        paciente.setNumeroCarnet(blankToNull(request.getNumeroCarnet()));
        
        if (request.getNumeroHistoriaClinica() != null && !request.getNumeroHistoriaClinica().isBlank()) {
            validarHistoriaClinicaUnica(request.getNumeroHistoriaClinica(), paciente.getId());
            paciente.setNumeroHistoriaClinica(request.getNumeroHistoriaClinica().trim());
        }
        
        if (request.getInstitucionCabeceraId() != null && request.getInstitucionCabeceraId() > 0) {
            paciente.setInstitucionCabecera(resolveInstitucion(request.getInstitucionCabeceraId()));
        } else if (request.getInstitucionCabeceraId() != null && request.getInstitucionCabeceraId() <= 0) {
            paciente.setInstitucionCabecera(null);
        }

        if (request.getMedicoCabeceraProfesionalId() != null && request.getMedicoCabeceraProfesionalId() > 0) {
            paciente.setMedicoCabecera(resolveProfesional(request.getMedicoCabeceraProfesionalId()));
        } else if (request.getMedicoCabeceraProfesionalId() != null && request.getMedicoCabeceraProfesionalId() <= 0) {
            paciente.setMedicoCabecera(null);
        }

        Paciente guardado = pacienteRepository.save(paciente);
        auditService.registrar("ADMIN_PACIENTE_EDICION", "pacientes", guardado.getId(), null, "Paciente actualizado");
        return toPacienteResponse(guardado);
    }

    @Transactional
    public void desactivarPaciente(Long id) {
        Paciente p = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));
        p.setActivo(false);
        p.getUsuario().setActivo(false);
        pacienteRepository.save(p);
        auditService.registrar("ADMIN_PACIENTE_BAJA", "pacientes", id, null, "Paciente desactivado");
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

    private void applyPacienteBase(Paciente paciente, String nombre, String apellido, String dni, java.time.LocalDate fechaNacimiento,
                                   String telefono, TipoSangre tipoSangre, ObraSocial obraSocial, String numeroCarnet,
                                   String numeroHistoriaClinica, Long institucionCabeceraId, Long medicoCabeceraProfesionalId,
                                   boolean activo) {
        paciente.setNombre(nombre.trim());
        paciente.setApellido(apellido.trim());
        paciente.setDni(dni.trim());
        paciente.setFechaNacimiento(fechaNacimiento);
        paciente.setTelefono(telefono.trim());
        paciente.setTipoSangre(tipoSangre);
        paciente.setObraSocial(obraSocial);
        paciente.setNumeroCarnet(blankToNull(numeroCarnet));
        paciente.setNumeroHistoriaClinica(numeroHistoriaClinica.trim());
        paciente.setInstitucionCabecera(institucionCabeceraId != null ? resolveInstitucion(institucionCabeceraId) : null);
        paciente.setMedicoCabecera(medicoCabeceraProfesionalId != null ? resolveProfesional(medicoCabeceraProfesionalId) : null);
        paciente.setActivo(activo);
    }

    private Set<Especialidad> resolveEspecialidades(List<Long> ids) {
        return ids.stream().map(id -> especialidadRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + id)))
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
                .orElseThrow(() -> new ResourceNotFoundException("Institución no encontrada con id: " + id));
    }

    private ObraSocial resolveObraSocial(Long id) {
        return obraSocialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Obra social no encontrada con id: " + id));
    }

    private Profesional resolveProfesional(Long id) {
        return profesionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + id));
    }

    private AdminUsuarioResponse toUsuarioResponse(Usuario usuario) {
        Long pacienteId = usuario.getPaciente() != null ? usuario.getPaciente().getId() : null;
        Long profesionalId = usuario.getProfesional() != null ? usuario.getProfesional().getId() : null;
        Long secretariaId = usuario.getSecretaria() != null ? usuario.getSecretaria().getId() : null;
        String nombreMostrar = usuario.getPaciente() != null ? usuario.getPaciente().getApellido() + ", " + usuario.getPaciente().getNombre()
                : usuario.getProfesional() != null ? usuario.getProfesional().getApellido() + ", " + usuario.getProfesional().getNombre()
                : usuario.getSecretaria() != null ? usuario.getSecretaria().getApellido() + ", " + usuario.getSecretaria().getNombre()
                : usuario.getEmail();
        String dni = usuario.getPaciente() != null ? usuario.getPaciente().getDni()
                : usuario.getProfesional() != null ? usuario.getProfesional().getDni()
                : usuario.getSecretaria() != null ? usuario.getSecretaria().getDni() : null;
        return new AdminUsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getRol(), usuario.getActivo(), usuario.getEmailVerificado(), pacienteId, profesionalId, secretariaId, nombreMostrar, dni);
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
