package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.PacientePerfilResponse;
import com.ramirez.mediturnosback.dto.PacientePerfilUpdateRequest;
import com.ramirez.mediturnosback.dto.PacienteUpdateRequest;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import com.ramirez.mediturnosback.service.JwtService;
import com.ramirez.mediturnosback.service.MediaFileService;
import com.ramirez.mediturnosback.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final JwtService jwtService;
    private final MediaFileService mediaFileService;
    private final CurrentUserService currentUserService;

    public PacienteController(PacienteService pacienteService, JwtService jwtService, MediaFileService mediaFileService, CurrentUserService currentUserService) {
        this.pacienteService = pacienteService;
        this.jwtService = jwtService;
        this.mediaFileService = mediaFileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<Paciente> listarTodos() {
        currentUserService.requireAnyRole(RolUsuario.ADMIN, RolUsuario.SECRETARY);
        return pacienteService.listarTodos();
    }

    @GetMapping("/{id}")
    public Paciente obtenerPorId(@PathVariable Long id) {
        validarAccesoPacienteId(id);
        return pacienteService.obtenerPorId(id);
    }

    @GetMapping("/buscar")
    public Paciente buscarPorDni(@RequestParam String dni) {
        currentUserService.requireAnyRole(RolUsuario.ADMIN, RolUsuario.SECRETARY, RolUsuario.PROFESSIONAL);
        return pacienteService.buscarPorDni(dni);
    }

    @GetMapping("/usuario/{usuarioId}")
    public Map<String, Long> obtenerPorUsuario(@PathVariable Long usuarioId) {
        validarAccesoUsuarioId(usuarioId);
        Paciente paciente = pacienteService.obtenerPorUsuarioId(usuarioId);
        return Map.of(
                "id", paciente.getId(),
                "pacienteId", paciente.getId(),
                "usuarioId", usuarioId
        );
    }

    @GetMapping("/perfil/me")
    public PacientePerfilResponse obtenerPerfilActual(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long usuarioId = jwtService.extraerUsuarioIdDesdeAuthorization(authorization);
        validarAccesoUsuarioId(usuarioId);
        return pacienteService.obtenerPerfilPorUsuarioId(usuarioId);
    }

    @GetMapping("/perfil/{usuarioId}")
    public PacientePerfilResponse obtenerPerfil(@PathVariable Long usuarioId) {
        validarAccesoUsuarioId(usuarioId);
        return pacienteService.obtenerPerfilPorUsuarioId(usuarioId);
    }

    @PostMapping(value = "/perfil/me/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PacientePerfilResponse subirFotoPerfilActual(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @RequestPart("file") MultipartFile file) {
        Long usuarioId = jwtService.extraerUsuarioIdDesdeAuthorization(authorization);
        validarAccesoUsuarioId(usuarioId);
        return pacienteService.actualizarFotoPerfil(usuarioId, file);
    }

    @PostMapping(value = "/perfil/me/carnet-obra-social", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PacientePerfilResponse subirCarnetObraSocialActual(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                              @RequestPart("file") MultipartFile file) {
        Long usuarioId = jwtService.extraerUsuarioIdDesdeAuthorization(authorization);
        validarAccesoUsuarioId(usuarioId);
        return pacienteService.actualizarCarnetObraSocial(usuarioId, file);
    }

    @GetMapping("/{id}/foto-perfil")
    public ResponseEntity<Resource> descargarFotoPerfil(@PathVariable Long id) {
        validarAccesoPacienteId(id);
        Paciente paciente = pacienteService.obtenerPorId(id);
        Resource resource = mediaFileService.loadAsResource(paciente.getFotoPerfilPath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(paciente.getFotoPerfilMimeType() != null ? paciente.getFotoPerfilMimeType() : "image/jpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"foto-perfil.jpg\"")
                .body(resource);
    }

    @GetMapping("/{id}/carnet-obra-social")
    public ResponseEntity<Resource> descargarCarnetObraSocial(@PathVariable Long id) {
        validarAccesoPacienteId(id);
        Paciente paciente = pacienteService.obtenerPorId(id);
        Resource resource = mediaFileService.loadAsResource(paciente.getCarnetObraSocialPath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(paciente.getCarnetObraSocialMimeType() != null ? paciente.getCarnetObraSocialMimeType() : "image/jpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"carnet-obra-social.jpg\"")
                .body(resource);
    }

    @PutMapping("/perfil/me")
    public PacientePerfilResponse actualizarPerfilActual(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                         @Valid @RequestBody PacientePerfilUpdateRequest request) {
        Long usuarioId = jwtService.extraerUsuarioIdDesdeAuthorization(authorization);
        validarAccesoUsuarioId(usuarioId);
        return pacienteService.actualizarPerfil(usuarioId, request);
    }

    @PutMapping("/perfil/{usuarioId}")
    public PacientePerfilResponse actualizarPerfil(@PathVariable Long usuarioId,
                                                   @Valid @RequestBody PacientePerfilUpdateRequest request) {
        validarAccesoUsuarioId(usuarioId);
        return pacienteService.actualizarPerfil(usuarioId, request);
    }

    @PutMapping("/{id}")
    public Paciente actualizar(@PathVariable Long id, @Valid @RequestBody PacienteUpdateRequest request) {
        currentUserService.requireAnyRole(RolUsuario.ADMIN, RolUsuario.SECRETARY);
        return pacienteService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        currentUserService.requireAnyRole(RolUsuario.ADMIN);
        pacienteService.eliminar(id);
    }

    private void validarAccesoUsuarioId(Long usuarioId) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isPatient() && user.usuarioId().equals(usuarioId)) return;
        throw new AccessDeniedException("No podés acceder a datos de otro paciente");
    }

    private void validarAccesoPacienteId(Long pacienteId) {
        AuthenticatedUser user = currentUserService.requireUser();
        if (user.isAdmin() || user.isSecretary()) return;
        if (user.isPatient() && user.pacienteId() != null && user.pacienteId().equals(pacienteId)) return;
        throw new AccessDeniedException("No podés acceder a datos de otro paciente");
    }
}
