package com.ramirez.mediturnosback.controller;

import com.ramirez.mediturnosback.model.ObraSocial;
import com.ramirez.mediturnosback.repository.ObraSocialRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/obras-sociales")
public class ObraSocialController {

    private final ObraSocialRepository obraSocialRepository;

    public ObraSocialController(ObraSocialRepository obraSocialRepository) {
        this.obraSocialRepository = obraSocialRepository;
    }

    @GetMapping
    public List<ObraSocial> listarActivas() {
        return obraSocialRepository.findByActivaTrueOrderByNombreAsc();
    }
}
