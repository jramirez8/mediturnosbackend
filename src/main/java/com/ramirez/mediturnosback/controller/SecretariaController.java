package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.TurnoReprogramacionRequest;
import com.ramirez.mediturnosback.dto.TurnoResponse;
import com.ramirez.mediturnosback.dto.TurnoSolicitudRequest;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.repository.TurnoRepository;
import com.ramirez.mediturnosback.service.PacienteService;
import com.ramirez.mediturnosback.service.TurnoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/secretaria")
public class SecretariaController {

    private final TurnoService turnoService;
    private final PacienteService pacienteService;
    private final TurnoRepository turnoRepository;

    public SecretariaController(TurnoService turnoService, PacienteService pacienteService, TurnoRepository turnoRepository) {
        this.turnoService = turnoService;
        this.pacienteService = pacienteService;
        this.turnoRepository = turnoRepository;
    }

    @GetMapping("/pacientes/buscar")
    public Paciente buscarPaciente(@RequestParam String dni) {
        return pacienteService.buscarPorDni(dni);
    }

    @GetMapping("/agenda")
    public List<TurnoResponse> agendaInstitucion(@RequestParam Long institucionId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return turnoRepository.findAgendaInstitucion(institucionId, fecha.atStartOfDay(), fecha.atTime(LocalTime.MAX))
                .stream().map(t -> turnoService.obtenerPorId(t.getId())).toList();
    }

    @PostMapping("/turnos")
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoResponse crearTurno(@Valid @RequestBody TurnoSolicitudRequest request) {
        return turnoService.solicitar(request);
    }

    @PutMapping("/turnos/{id}/reprogramar")
    public TurnoResponse reprogramar(@PathVariable Long id, @Valid @RequestBody TurnoReprogramacionRequest request) {
        return turnoService.reprogramar(id, request);
    }
}
