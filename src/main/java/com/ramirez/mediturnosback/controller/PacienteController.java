package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.PacientePerfilResponse;
import com.ramirez.mediturnosback.dto.PacientePerfilUpdateRequest;
import com.ramirez.mediturnosback.dto.PacienteUpdateRequest;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.service.PacienteService;
import com.ramirez.mediturnosback.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final JwtService jwtService;

    public PacienteController(PacienteService pacienteService, JwtService jwtService) {
        this.pacienteService = pacienteService;
        this.jwtService = jwtService;
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
