package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.*;
import com.ramirez.mediturnosback.model.Especialidad;
import com.ramirez.mediturnosback.model.Institucion;
import com.ramirez.mediturnosback.model.ObraSocial;
import com.ramirez.mediturnosback.model.RolUsuario;
import com.ramirez.mediturnosback.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() { return adminService.resumen(); }

    @GetMapping("/roles")
    public List<RolUsuario> roles() { return adminService.roles(); }

    @GetMapping("/usuarios")
    public List<AdminUsuarioResponse> listarUsuarios() { return adminService.listarUsuarios(); }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUsuarioResponse crearUsuario(@Valid @RequestBody AdminUsuarioCreateRequest request) { return adminService.crearUsuario(request); }

    @PutMapping("/usuarios/{id}")
    public AdminUsuarioResponse actualizarUsuario(@PathVariable Long id, @Valid @RequestBody AdminUsuarioUpdateRequest request) {
        return adminService.actualizarUsuario(id, request);
    }

    @DeleteMapping("/usuarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarUsuario(@PathVariable Long id) { adminService.desactivarUsuario(id); }

    @PostMapping("/usuarios/{id}/reenviar-verificacion")
    public Map<String, Object> reenviarVerificacionUsuario(@PathVariable Long id) {
        return adminService.reenviarVerificacionUsuario(id);
    }

    @GetMapping("/instituciones")
    public List<Institucion> listarInstituciones() { return adminService.listarInstituciones(); }

    @PostMapping("/instituciones")
    @ResponseStatus(HttpStatus.CREATED)
    public Institucion crearInstitucion(@Valid @RequestBody AdminInstitucionRequest request) { return adminService.crearInstitucion(request); }

    @PutMapping("/instituciones/{id}")
    public Institucion actualizarInstitucion(@PathVariable Long id, @Valid @RequestBody AdminInstitucionRequest request) { return adminService.actualizarInstitucion(id, request); }

    @DeleteMapping("/instituciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarInstitucion(@PathVariable Long id) { adminService.desactivarInstitucion(id); }

    @GetMapping("/especialidades")
    public List<Especialidad> listarEspecialidades() { return adminService.listarEspecialidades(); }

    @PostMapping("/especialidades")
    @ResponseStatus(HttpStatus.CREATED)
    public Especialidad crearEspecialidad(@Valid @RequestBody AdminEspecialidadRequest request) { return adminService.crearEspecialidad(request); }

    @PutMapping("/especialidades/{id}")
    public Especialidad actualizarEspecialidad(@PathVariable Long id, @Valid @RequestBody AdminEspecialidadRequest request) { return adminService.actualizarEspecialidad(id, request); }

    @DeleteMapping("/especialidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarEspecialidad(@PathVariable Long id) { adminService.desactivarEspecialidad(id); }

    @GetMapping("/obras-sociales")
    public List<ObraSocial> listarObrasSociales() { return adminService.listarObrasSociales(); }

    @PostMapping("/obras-sociales")
    @ResponseStatus(HttpStatus.CREATED)
    public ObraSocial crearObraSocial(@Valid @RequestBody AdminObraSocialRequest request) { return adminService.crearObraSocial(request); }

    @PutMapping("/obras-sociales/{id}")
    public ObraSocial actualizarObraSocial(@PathVariable Long id, @Valid @RequestBody AdminObraSocialRequest request) { return adminService.actualizarObraSocial(id, request); }

    @DeleteMapping("/obras-sociales/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarObraSocial(@PathVariable Long id) { adminService.desactivarObraSocial(id); }

    @GetMapping("/profesionales")
    public List<AdminProfesionalResponse> listarProfesionales() { return adminService.listarProfesionales(); }

    @PostMapping("/profesionales")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProfesionalResponse crearProfesional(@Valid @RequestBody AdminProfesionalRequest request) { return adminService.crearProfesional(request); }

    @PutMapping("/profesionales/{id}")
    public AdminProfesionalResponse actualizarProfesional(@PathVariable Long id, @Valid @RequestBody AdminProfesionalUpdateRequest request) { return adminService.actualizarProfesional(id, request); }

    @DeleteMapping("/profesionales/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarProfesional(@PathVariable Long id) { adminService.desactivarProfesional(id); }

    @GetMapping("/secretarias")
    public List<AdminSecretariaResponse> listarSecretarias() { return adminService.listarSecretarias(); }

    @PostMapping("/secretarias")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminSecretariaResponse crearSecretaria(@Valid @RequestBody AdminSecretariaRequest request) { return adminService.crearSecretaria(request); }

    @PutMapping("/secretarias/{id}")
    public AdminSecretariaResponse actualizarSecretaria(@PathVariable Long id, @Valid @RequestBody AdminSecretariaUpdateRequest request) { return adminService.actualizarSecretaria(id, request); }

    @DeleteMapping("/secretarias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarSecretaria(@PathVariable Long id) { adminService.desactivarSecretaria(id); }

    @GetMapping("/pacientes")
    public List<AdminPacienteResponse> listarPacientes() { return adminService.listarPacientes(); }

    @PostMapping("/pacientes")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPacienteResponse crearPaciente(@Valid @RequestBody AdminPacienteRequest request) { return adminService.crearPaciente(request); }

    @PutMapping("/pacientes/{id}")
    public AdminPacienteResponse actualizarPaciente(@PathVariable Long id, @Valid @RequestBody AdminPacienteUpdateRequest request) { return adminService.actualizarPaciente(id, request); }

    @DeleteMapping("/pacientes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarPaciente(@PathVariable Long id) { adminService.desactivarPaciente(id); }
}
