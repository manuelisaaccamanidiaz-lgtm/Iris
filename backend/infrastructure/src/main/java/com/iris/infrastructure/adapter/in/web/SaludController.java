package com.iris.infrastructure.adapter.in.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada (web) mínimo para comprobar que la API responde.
 * GET /api/salud -> {"estado":"ok","servicio":"iris"}
 */
@RestController
@RequestMapping("/api/salud")
public class SaludController {

    @GetMapping
    public Map<String, String> salud() {
        return Map.of("estado", "ok", "servicio", "iris");
    }
}
