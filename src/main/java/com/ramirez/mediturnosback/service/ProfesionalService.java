package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.ProfesionalDto;
import com.ramirez.mediturnosback.exception.ResourceNotFoundException;
import com.ramirez.mediturnosback.model.Profesional;
import com.ramirez.mediturnosback.model.ProfesionalInstitucion;
import com.ramirez.mediturnosback.repository.ProfesionalInstitucionRepository;
import com.ramirez.mediturnosback.repository.ProfesionalRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ProfesionalService {

    private final ProfesionalRepository profesionalRepository;
    private final ProfesionalInstitucionRepository profesionalInstitucionRepository;

    public ProfesionalService(ProfesionalRepository profesionalRepository,
                              ProfesionalInstitucionRepository profesionalInstitucionRepository) {
        this.profesionalRepository = profesionalRepository;
        this.profesionalInstitucionRepository = profesionalInstitucionRepository;
    }

    public List<ProfesionalDto> listar(String especialidad, String filtro) {
        String especialidadFiltro = (especialidad == null || especialidad.isBlank()) ? null : especialidad.trim();
        String q = (filtro == null || filtro.isBlank()) ? null : filtro.trim();
        return profesionalInstitucionRepository.buscarDisponibles(especialidadFiltro, q)
                .stream()
                .sorted(Comparator.comparing((ProfesionalInstitucion pi) -> pi.getProfesional().getApellido(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(pi -> pi.getProfesional().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .map(pi -> {
                    Profesional p = pi.getProfesional();
                    String especialidadNombre = p.getEspecialidades().stream()
                            .filter(e -> especialidadFiltro == null || e.getNombre().equalsIgnoreCase(especialidadFiltro))
                            .map(e -> e.getNombre())
                            .findFirst()
                            .orElseGet(() -> p.getEspecialidades().stream().findFirst().map(e -> e.getNombre()).orElse(null));
                    Long especialidadId = p.getEspecialidades().stream()
                            .filter(e -> especialidadFiltro == null || e.getNombre().equalsIgnoreCase(especialidadFiltro))
                            .map(e -> e.getId())
                            .findFirst()
                            .orElseGet(() -> p.getEspecialidades().stream().findFirst().map(e -> e.getId()).orElse(null));
                    return new ProfesionalDto(
                            p.getId(),
                            pi.getId(),
                            pi.getInstitucion().getId(),
                            especialidadId,
                            p.getNombre(),
                            p.getApellido(),
                            p.getMatricula(),
                            especialidadNombre,
                            firstNonBlank(pi.getTelefonoEnSede(), p.getTelefono(), pi.getInstitucion().getTelefono()),
                            pi.getInstitucion().getNombre(),
                            pi.getInstitucion().getDireccion(),
                            p.getEmailCuenta(),
                            p.getNombreCompleto()
                    );
                })
                .toList();
    }

    public List<String> listarEspecialidades() {
        return profesionalRepository.listarNombresEspecialidades();
    }

    public Profesional obtenerPorId(Long id) {
        return profesionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + id));
    }

    public Profesional obtenerPorUsuarioId(Long usuarioId) {
        return profesionalRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado para el usuario: " + usuarioId));
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
