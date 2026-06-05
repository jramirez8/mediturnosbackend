package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.service.AgendaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {
    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping("/horarios")
    public List<HorarioAtencionResponse> horarios(@RequestParam Long profesionalInstitucionId) {
        return agendaService.horarios(profesionalInstitucionId);
    }

    @PostMapping("/horarios")
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioAtencionResponse crearHorario(@RequestBody HorarioAtencionRequest request) {
        return agendaService.crearHorario(request);
    }

    @DeleteMapping("/horarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarHorario(@PathVariable Long id) {
        agendaService.desactivarHorario(id);
    }

    @GetMapping("/bloqueos")
    public List<AgendaBloqueoResponse> bloqueos(@RequestParam Long profesionalInstitucionId) {
        return agendaService.bloqueos(profesionalInstitucionId);
    }

    @PostMapping("/bloqueos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendaBloqueoResponse crearBloqueo(@RequestBody AgendaBloqueoRequest request) {
        return agendaService.crearBloqueo(request);
    }

    @DeleteMapping("/bloqueos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBloqueo(@PathVariable Long id) {
        agendaService.eliminarBloqueo(id);
    }
}
