package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.ProfesionalDto;
import com.ramirez.mediturnosback.dto.TurnoResponse;
import com.ramirez.mediturnosback.service.ProfesionalService;
import com.ramirez.mediturnosback.service.TurnoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/profesionales")
public class ProfesionalController {

    private final ProfesionalService profesionalService;
    private final TurnoService turnoService;

    public ProfesionalController(ProfesionalService profesionalService, TurnoService turnoService) {
        this.profesionalService = profesionalService;
        this.turnoService = turnoService;
    }

    @GetMapping
    public List<ProfesionalDto> listarTodos(@RequestParam(required = false) String especialidad,
                                            @RequestParam(required = false, name = "q") String filtro) {
        return profesionalService.listar(especialidad, filtro);
    }

    @GetMapping("/especialidades")
    public List<String> listarEspecialidades() { return profesionalService.listarEspecialidades(); }

    @GetMapping("/agenda/{usuarioId}")
    public List<TurnoResponse> agenda(@PathVariable Long usuarioId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return turnoService.agendaProfesional(usuarioId, fecha);
    }

    @GetMapping("/proximo-turno/{usuarioId}")
    public TurnoResponse proximoTurno(@PathVariable Long usuarioId) {
        return turnoService.proximoTurnoProfesional(usuarioId);
    }

    @GetMapping("/historial-paciente")
    public List<TurnoResponse> historialPaciente(@RequestParam String dni) {
        return turnoService.historiaPorDni(dni);
    }
}
