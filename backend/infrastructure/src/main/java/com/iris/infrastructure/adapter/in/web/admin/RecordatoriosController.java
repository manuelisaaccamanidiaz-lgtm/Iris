package com.iris.infrastructure.adapter.in.web.admin;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iris.infrastructure.adapter.in.programado.Despertador;

/**
 * Solo ADMIN (SecurityConfig protege /api/admin/**): dispara el despertador YA, para la demo.
 * Es idempotente: llamarlo dos veces no duplica recordatorios.
 */
@RestController
@RequestMapping("/api/admin/recordatorios")
class RecordatoriosController {

    private final Despertador despertador;

    RecordatoriosController(Despertador despertador) {
        this.despertador = despertador;
    }

    @PostMapping("/ejecutar")
    Despertador.Resultado ejecutar() {
        return despertador.despertar();
    }
}
