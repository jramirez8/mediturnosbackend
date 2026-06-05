package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.PacientePerfilResponse;
import com.ramirez.mediturnosback.dto.PacientePerfilUpdateRequest;
import com.ramirez.mediturnosback.dto.PacienteUpdateRequest;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.service.PacienteService;
import com.ramirez.mediturnosback.service.MediaFileService;
import com.ramirez.mediturnosback.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public PacienteController(PacienteService pacienteService, JwtService jwtService, MediaFileService mediaFileService) {
        this.pacienteService = pacienteService;
        this.jwtService = jwtService;
        this.mediaFileService = mediaFileService;
    }

    @GetMapping
    public List<Paciente> listarTodos() {
        return pacienteService.listarTodos();
    }

    @GetMapping("/{id}")
    public Paciente obtenerPorId(@PathVariable Long id) {
        return pacienteService.obtenerPorId(id);
    }

    @GetMapping("/buscar")
    public Paciente buscarPorDni(@RequestParam String dni) {
        return pacienteService.buscarPorDni(dni);
    }

    @GetMapping("/usuario/{usuarioId}")
    public Map<String, Long> obtenerPorUsuario(@PathVariable Long usuarioId) {
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
        return pacienteService.obtenerPerfilPorUsuarioId(usuarioId);
    }

    @GetMapping("/perfil/{usuarioId}")
    public PacientePerfilResponse obtenerPerfil(@PathVariable Long usuarioId) {
        return pacienteService.obtenerPerfilPorUsuarioId(usuarioId);
    }


    @PostMapping(value = "/perfil/me/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PacientePerfilResponse subirFotoPerfilActual(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @RequestPart("file") MultipartFile file) {
        Long usuarioId = jwtService.extraerUsuarioIdDesdeAuthorization(authorization);
        return pacienteService.actualizarFotoPerfil(usuarioId, file);
    }

    @PostMapping(value = "/perfil/me/carnet-obra-social", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PacientePerfilResponse subirCarnetObraSocialActual(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                              @RequestPart("file") MultipartFile file) {
        Long usuarioId = jwtService.extraerUsuarioIdDesdeAuthorization(authorization);
        return pacienteService.actualizarCarnetObraSocial(usuarioId, file);
    }

    @GetMapping("/{id}/foto-perfil")
    public ResponseEntity<Resource> descargarFotoPerfil(@PathVariable Long id) {
        Paciente paciente = pacienteService.obtenerPorId(id);
        Resource resource = mediaFileService.loadAsResource(paciente.getFotoPerfilPath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(paciente.getFotoPerfilMimeType() != null ? paciente.getFotoPerfilMimeType() : "image/jpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"foto-perfil.jpg\"")
                .body(resource);
    }

    @GetMapping("/{id}/carnet-obra-social")
    public ResponseEntity<Resource> descargarCarnetObraSocial(@PathVariable Long id) {
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
        return pacienteService.actualizarPerfil(usuarioId, request);
    }

    @PutMapping("/perfil/{usuarioId}")
    public PacientePerfilResponse actualizarPerfil(@PathVariable Long usuarioId,
                                                   @Valid @RequestBody PacientePerfilUpdateRequest request) {
        return pacienteService.actualizarPerfil(usuarioId, request);
    }

    @PutMapping("/{id}")
    public Paciente actualizar(@PathVariable Long id, @Valid @RequestBody PacienteUpdateRequest request) {
        return pacienteService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        pacienteService.eliminar(id);
    }
}
