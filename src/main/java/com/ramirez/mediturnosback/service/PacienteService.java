package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.PacientePerfilResponse;
import com.ramirez.mediturnosback.dto.PacientePerfilUpdateRequest;
import com.ramirez.mediturnosback.dto.PacienteUpdateRequest;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.Institucion;
import com.ramirez.mediturnosback.model.ObraSocial;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.model.Profesional;
import com.ramirez.mediturnosback.model.Usuario;
import com.ramirez.mediturnosback.repository.InstitucionRepository;
import com.ramirez.mediturnosback.repository.ObraSocialRepository;
import com.ramirez.mediturnosback.repository.PacienteRepository;
import com.ramirez.mediturnosback.repository.ProfesionalRepository;
import com.ramirez.mediturnosback.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final ObraSocialRepository obraSocialRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstitucionRepository institucionRepository;
    private final ProfesionalRepository profesionalRepository;
    private final MediaFileService mediaFileService;

    public PacienteService(PacienteRepository pacienteRepository,
                           ObraSocialRepository obraSocialRepository,
                           UsuarioRepository usuarioRepository,
                           InstitucionRepository institucionRepository,
                           ProfesionalRepository profesionalRepository,
                           MediaFileService mediaFileService) {
        this.pacienteRepository = pacienteRepository;
        this.obraSocialRepository = obraSocialRepository;
        this.usuarioRepository = usuarioRepository;
        this.institucionRepository = institucionRepository;
        this.profesionalRepository = profesionalRepository;
        this.mediaFileService = mediaFileService;
    }

    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }

    public Paciente obtenerPorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));
    }

    public Paciente obtenerPorUsuarioId(Long usuarioId) {
        return pacienteRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado para el usuario: " + usuarioId));
    }

    public Paciente buscarPorDni(String dni) {
        return pacienteRepository.findByDni(dni)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con DNI: " + dni));
    }

    @Transactional(readOnly = true)
    public PacientePerfilResponse obtenerPerfilPorUsuarioId(Long usuarioId) {
        Paciente paciente = obtenerPorUsuarioId(usuarioId);
        return mapPerfil(paciente);
    }

    @Transactional
    public Paciente actualizar(Long id, PacienteUpdateRequest request) {
        Paciente pacienteExistente = obtenerPorId(id);
        Usuario usuario = pacienteExistente.getUsuario();
        ObraSocial obraSocial = obraSocialRepository.findById(request.getObraSocialId())
                .orElseThrow(() -> new ResourceNotFoundException("Obra social no encontrada con id: " + request.getObraSocialId()));

        validarDatosUnicosActualizacion(pacienteExistente, usuario, request);

        pacienteExistente.setNombre(request.getNombre().trim());
        pacienteExistente.setApellido(request.getApellido().trim());
        pacienteExistente.setDni(request.getDni().trim());
        pacienteExistente.setFechaNacimiento(request.getFechaNacimiento());
        pacienteExistente.setTelefono(request.getTelefono().trim());
        pacienteExistente.setTipoSangre(request.getTipoSangre());
        pacienteExistente.setNumeroCarnet(normalizarOpcional(request.getNumeroCarnet()));
        pacienteExistente.setNumeroHistoriaClinica(request.getNumeroHistoriaClinica().trim());
        pacienteExistente.setInstitucionCabecera(resolverInstitucionPorNombre(request.getHospitalClinicaCabecera()));
        pacienteExistente.setMedicoCabecera(resolverProfesionalPorNombre(request.getDoctorCabecera()));
        pacienteExistente.setObraSocial(obraSocial);

        usuario.setEmail(request.getEmail().trim().toLowerCase());
        usuarioRepository.save(usuario);
        return pacienteRepository.save(pacienteExistente);
    }

    @Transactional
    public PacientePerfilResponse actualizarPerfil(Long usuarioId, PacientePerfilUpdateRequest request) {
        Paciente paciente = obtenerPorUsuarioId(usuarioId);
        Usuario usuario = paciente.getUsuario();
        ObraSocial obraSocial = obraSocialRepository.findById(request.getObraSocialId())
                .orElseThrow(() -> new ResourceNotFoundException("Obra social no encontrada con id: " + request.getObraSocialId()));

        String nuevoEmail = request.getEmail().trim().toLowerCase();
        if (!usuario.getEmail().equalsIgnoreCase(nuevoEmail) && usuarioRepository.existsByEmailIgnoreCase(nuevoEmail)) {
            throw new IllegalArgumentException("Ya existe otro usuario con ese email");
        }

        usuario.setEmail(nuevoEmail);
        paciente.setTelefono(request.getTelefono().trim());
        paciente.setObraSocial(obraSocial);
        paciente.setNumeroCarnet(normalizarOpcional(request.getNumeroCarnet()));
        paciente.setInstitucionCabecera(resolverInstitucionPorNombre(request.getHospitalClinicaCabecera()));
        paciente.setMedicoCabecera(resolverProfesionalPorNombre(request.getDoctorCabecera()));
        paciente.setFotoPerfilBase64(normalizarOpcional(request.getFotoPerfilBase64()));
        paciente.setCarnetObraSocialBase64(normalizarOpcional(request.getCarnetObraSocialBase64()));

        usuarioRepository.save(usuario);
        return mapPerfil(pacienteRepository.save(paciente));
    }

    @Transactional
    public PacientePerfilResponse actualizarFotoPerfil(Long usuarioId, MultipartFile file) {
        Paciente paciente = obtenerPorUsuarioId(usuarioId);
        MediaFileService.StoredImage stored = mediaFileService.storeCompressedImage(file, "pacientes/foto-perfil", paciente.getId());
        mediaFileService.deleteQuietly(paciente.getFotoPerfilPath());
        paciente.setFotoPerfilPath(stored.relativePath());
        paciente.setFotoPerfilMimeType(stored.mimeType());
        paciente.setFotoPerfilSizeBytes(stored.compressedSizeBytes());
        paciente.setFotoPerfilBase64(null);
        return mapPerfil(pacienteRepository.save(paciente));
    }

    @Transactional
    public PacientePerfilResponse actualizarCarnetObraSocial(Long usuarioId, MultipartFile file) {
        Paciente paciente = obtenerPorUsuarioId(usuarioId);
        MediaFileService.StoredImage stored = mediaFileService.storeCompressedImage(file, "pacientes/carnet-obra-social", paciente.getId());
        mediaFileService.deleteQuietly(paciente.getCarnetObraSocialPath());
        paciente.setCarnetObraSocialPath(stored.relativePath());
        paciente.setCarnetObraSocialMimeType(stored.mimeType());
        paciente.setCarnetObraSocialSizeBytes(stored.compressedSizeBytes());
        paciente.setCarnetObraSocialBase64(null);
        return mapPerfil(pacienteRepository.save(paciente));
    }

    @Transactional
    public void eliminar(Long id) {
        pacienteRepository.delete(obtenerPorId(id));
    }

    private void validarDatosUnicosActualizacion(Paciente pacienteExistente, Usuario usuario, PacienteUpdateRequest request) {
        String nuevoDni = request.getDni().trim();
        String nuevoEmail = request.getEmail().trim().toLowerCase();
        String nuevaHistoria = request.getNumeroHistoriaClinica().trim();

        if (!pacienteExistente.getDni().equals(nuevoDni) && pacienteRepository.existsByDni(nuevoDni)) {
            throw new IllegalArgumentException("Ya existe otro paciente con ese DNI");
        }
        if (!usuario.getEmail().equalsIgnoreCase(nuevoEmail) && usuarioRepository.existsByEmailIgnoreCase(nuevoEmail)) {
            throw new IllegalArgumentException("Ya existe otro usuario con ese email");
        }
        if (!pacienteExistente.getNumeroHistoriaClinica().equals(nuevaHistoria) && pacienteRepository.existsByNumeroHistoriaClinica(nuevaHistoria)) {
            throw new IllegalArgumentException("Ya existe otro paciente con ese número de historia clínica");
        }
    }

    private Institucion resolverInstitucionPorNombre(String nombre) {
        String valor = normalizarOpcional(nombre);
        if (valor == null) return null;
        return institucionRepository.findByNombreIgnoreCase(valor).orElse(null);
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

    private PacientePerfilResponse mapPerfil(Paciente paciente) {
        return new PacientePerfilResponse(
                paciente.getUsuario().getId(),
                paciente.getId(),
                paciente.getNombre(),
                paciente.getApellido(),
                paciente.getDni(),
                paciente.getFechaNacimiento(),
                paciente.getTipoSangre(),
                paciente.getUsuario().getEmail(),
                paciente.getTelefono(),
                paciente.getObraSocial() != null ? paciente.getObraSocial().getId() : null,
                paciente.getObraSocial() != null ? paciente.getObraSocial().getNombre() : null,
                paciente.getNumeroCarnet(),
                paciente.getNumeroHistoriaClinica(),
                paciente.getInstitucionCabecera() != null ? paciente.getInstitucionCabecera().getId() : null,
                paciente.getHospitalClinicaCabecera(),
                paciente.getMedicoCabecera() != null ? paciente.getMedicoCabecera().getId() : null,
                paciente.getDoctorCabecera(),
                paciente.getUsuario().getEmailVerificado(),
                paciente.getFotoPerfilBase64(),
                paciente.getCarnetObraSocialBase64(),
                paciente.getFotoPerfilPath() != null ? "/api/pacientes/" + paciente.getId() + "/foto-perfil" : null,
                paciente.getCarnetObraSocialPath() != null ? "/api/pacientes/" + paciente.getId() + "/carnet-obra-social" : null,
                paciente.getFotoPerfilSizeBytes(),
                paciente.getCarnetObraSocialSizeBytes()
        );
    }
}
