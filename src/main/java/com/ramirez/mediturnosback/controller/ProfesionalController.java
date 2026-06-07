package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.ProfesionalDto;
import com.ramirez.mediturnosback.dto.TurnoResponse;
import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.security.AuthenticatedUser;
import com.ramirez.mediturnosback.security.CurrentUserService;
import com.ramirez.mediturnosback.service.ProfesionalService;
import com.ramirez.mediturnosback.service.TurnoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/profesionales")
public class ProfesionalController {

    private final ProfesionalService profesionalService;
    private final TurnoService turnoService;
    private final CurrentUserService currentUserService;

    public ProfesionalController(ProfesionalService profesionalService, TurnoService turnoService, CurrentUserService currentUserService) {
        this.profesionalService = profesionalService;
        this.turnoService = turnoService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ProfesionalDto> listarTodos(@RequestParam(required = false) String especialidad,
                                            @RequestParam(required = false, name = "q") String filtro) {
        return profesionalService.listar(especialidad, filtro);
    }

    @GetMapping("/especialidades")
    public List<String> listarEspecialidades() { return profesionalService.listarEspecialidades(); }

    @GetMapping("/me")
    public ProfesionalDto me() {
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.PROFESSIONAL, RolUsuario.ADMIN);
        return profesionalService.obtenerPerfilActual(user.usuarioId());
    }

    @GetMapping("/me/sedes")
    public List<ProfesionalDto> misSedes() {
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.PROFESSIONAL, RolUsuario.ADMIN);
        return profesionalService.listarSedesPorUsuarioId(user.usuarioId());
    }

    @GetMapping("/agenda/me")
    public List<TurnoResponse> agendaPropia(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.PROFESSIONAL, RolUsuario.ADMIN);
        return turnoService.agendaProfesional(user.usuarioId(), fecha);
    }

    @GetMapping("/agenda/{usuarioId}")
    public List<TurnoResponse> agenda(@PathVariable Long usuarioId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        validarProfesionalSolicitado(usuarioId);
        return turnoService.agendaProfesional(usuarioId, fecha);
    }

    @GetMapping("/proximo-turno/me")
    public TurnoResponse proximoTurnoPropio() {
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.PROFESSIONAL, RolUsuario.ADMIN);
        return turnoService.proximoTurnoProfesional(user.usuarioId());
    }

    @GetMapping("/proximo-turno/{usuarioId}")
    public TurnoResponse proximoTurno(@PathVariable Long usuarioId) {
        validarProfesionalSolicitado(usuarioId);
        return turnoService.proximoTurnoProfesional(usuarioId);
    }

    @GetMapping("/historial-paciente")
    public List<TurnoResponse> historialPaciente(@RequestParam String dni) {
        currentUserService.requireAnyRole(RolUsuario.PROFESSIONAL, RolUsuario.ADMIN, RolUsuario.SECRETARY);
        return turnoService.historiaPorDni(dni);
    }

    private void validarProfesionalSolicitado(Long usuarioId) {
        AuthenticatedUser user = currentUserService.requireAnyRole(RolUsuario.PROFESSIONAL, RolUsuario.ADMIN);
        if (user.isProfessional() && !user.usuarioId().equals(usuarioId)) {
            throw new AccessDeniedException("No podés consultar la agenda de otro profesional");
        }
    }
}
