package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.service.TurnoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
public class TurnoController {

    private final TurnoService turnoService;

    public TurnoController(TurnoService turnoService) { this.turnoService = turnoService; }

    @GetMapping
    public List<TurnoResponse> listarTodos() { return turnoService.listarTodos(); }

    @GetMapping("/{id}")
    public TurnoResponse obtenerPorId(@PathVariable Long id) { return turnoService.obtenerPorId(id); }

    @GetMapping("/paciente/{pacienteId}")
    public List<TurnoResponse> listarPorPaciente(@PathVariable Long pacienteId) { return turnoService.listarPorPaciente(pacienteId); }

    @GetMapping("/historia-clinica/{usuarioId}")
    public List<TurnoResponse> listarHistoriaClinica(@PathVariable Long usuarioId) { return turnoService.listarHistoriaClinica(usuarioId); }

    @GetMapping("/disponibilidad")
    public List<DisponibilidadSlotResponse> listarDisponibilidad(@RequestParam Long profesionalInstitucionId) {
        return turnoService.listarDisponibilidad(profesionalInstitucionId);
    }

    @PostMapping("/solicitar")
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoResponse solicitar(@Valid @RequestBody TurnoSolicitudRequest request) { return turnoService.solicitar(request); }

    @PutMapping("/{id}/reprogramar")
    public TurnoResponse reprogramar(@PathVariable Long id, @Valid @RequestBody TurnoReprogramacionRequest request) {
        return turnoService.reprogramar(id, request);
    }

    @PutMapping("/{id}/estado")
    public TurnoResponse actualizarEstado(@PathVariable Long id, @Valid @RequestBody TurnoEstadoUpdateRequest request) {
        return turnoService.actualizarEstado(id, request);
    }

    @PutMapping("/{id}/detalle-consulta")
    public TurnoResponse cargarDetalleConsulta(@PathVariable Long id, @RequestBody DetalleConsultaRequest request) {
        return turnoService.cargarDetalleConsulta(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { turnoService.eliminar(id); }
}
