package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.dto.InstitucionDto;
import com.ramirez.mediturnosback.repository.InstitucionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instituciones")
public class InstitucionController {

    private final InstitucionRepository institucionRepository;

    public InstitucionController(InstitucionRepository institucionRepository) {
        this.institucionRepository = institucionRepository;
    }

    @GetMapping
    public List<InstitucionDto> listar() {
        return institucionRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(i -> new InstitucionDto(i.getId(), i.getNombre(), i.getDireccion(), i.getTelefono(), i.getWhatsapp(), i.getTipo().name()))
                .toList();
    }
}
