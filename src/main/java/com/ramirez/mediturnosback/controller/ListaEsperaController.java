package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.ListaEsperaRequest;
import com.ramirez.mediturnosback.dto.ListaEsperaResponse;
import com.ramirez.mediturnosback.service.JwtService;
import com.ramirez.mediturnosback.service.ListaEsperaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lista-espera")
public class ListaEsperaController {
    private final ListaEsperaService listaEsperaService;
    private final JwtService jwtService;

    public ListaEsperaController(ListaEsperaService listaEsperaService, JwtService jwtService) {
        this.listaEsperaService = listaEsperaService;
        this.jwtService = jwtService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListaEsperaResponse crear(@RequestBody ListaEsperaRequest request) {
        return listaEsperaService.crear(request);
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<ListaEsperaResponse> porPaciente(@PathVariable Long pacienteId) {
        return listaEsperaService.listarPorPaciente(pacienteId);
    }

    @GetMapping("/me")
    public List<ListaEsperaResponse> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long pacienteId = jwtService.extraerPacienteIdDesdeAuthorization(authorization);
        return listaEsperaService.listarPorPaciente(pacienteId);
    }

    @GetMapping("/pendientes")
    public List<ListaEsperaResponse> pendientes() {
        return listaEsperaService.listarPendientes();
    }
}
