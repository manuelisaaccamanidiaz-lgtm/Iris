package com.iris.infrastructure.adapter.in.web.evento;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.evento.CrearEvento;
import com.iris.application.evento.ListarEventos;
import com.iris.infrastructure.adapter.in.web.evento.EventoDtos.RespuestaEvento;
import com.iris.infrastructure.adapter.in.web.evento.EventoDtos.SolicitudEvento;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos")
class EventoController {

    private final CrearEvento crearEvento;
    private final ListarEventos listarEventos;

    EventoController(CrearEvento crearEvento, ListarEventos listarEventos) {
        this.crearEvento = crearEvento;
        this.listarEventos = listarEventos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaEvento crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SolicitudEvento s) {
        UUID usuarioId = UUID.fromString(jwt.getSubject()); // el "quién" sale del token
        var comando = new CrearEvento.Comando(s.nombre(), s.descripcion(), s.lugar(), s.fechaInicio(), s.fechaFin());
        return RespuestaEvento.de(crearEvento.ejecutar(usuarioId, comando));
    }

    @GetMapping
    List<RespuestaEvento> listar(@AuthenticationPrincipal Jwt jwt) {
        UUID usuarioId = UUID.fromString(jwt.getSubject());
        return listarEventos.ejecutar(usuarioId).stream().map(RespuestaEvento::de).toList();
    }
}
