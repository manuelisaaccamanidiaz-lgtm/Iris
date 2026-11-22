package com.iris.infrastructure.adapter.in.web.agenda;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.agenda.CrearSesion;
import com.iris.application.agenda.EditarSesion;
import com.iris.application.agenda.EliminarSesion;
import com.iris.application.agenda.ListarSesiones;
import com.iris.domain.agenda.Sesion;
import com.iris.domain.agenda.TipoSesion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** La agenda del evento. Horas en formato "09:00", fechas en "2026-05-20". */
@RestController
@RequestMapping("/api/eventos/{eventoId}/sesiones")
class SesionController {

    private final CrearSesion crear;
    private final ListarSesiones listar;
    private final EditarSesion editar;
    private final EliminarSesion eliminar;

    SesionController(CrearSesion crear, ListarSesiones listar, EditarSesion editar, EliminarSesion eliminar) {
        this.crear = crear;
        this.listar = listar;
        this.editar = editar;
        this.eliminar = eliminar;
    }

    @GetMapping
    List<RespuestaSesion> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId) {
        return listar.ejecutar(usuario(jwt), eventoId).stream().map(RespuestaSesion::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaSesion crear(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                          @Valid @RequestBody SolicitudSesion s) {
        return RespuestaSesion.de(crear.ejecutar(usuario(jwt), eventoId, s.aDatos()));
    }

    @PutMapping("/{sesionId}")
    RespuestaSesion editar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                           @PathVariable UUID sesionId, @Valid @RequestBody SolicitudSesion s) {
        return RespuestaSesion.de(editar.ejecutar(usuario(jwt), eventoId, sesionId, s.aDatos()));
    }

    @DeleteMapping("/{sesionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204: borrado, sin nada que devolver
    void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId, @PathVariable UUID sesionId) {
        eliminar.ejecutar(usuario(jwt), eventoId, sesionId);
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    record SolicitudSesion(@NotBlank String titulo, String descripcion, @NotNull LocalDate fecha,
                           @NotNull LocalTime horaInicio, @NotNull LocalTime horaFin, String sala,
                           String ponente, TipoSesion tipo) {

        Sesion.Datos aDatos() {
            return new Sesion.Datos(titulo, descripcion, fecha, horaInicio, horaFin, sala, ponente, tipo);
        }
    }

    record RespuestaSesion(UUID id, String titulo, String descripcion, LocalDate fecha, LocalTime horaInicio,
                           LocalTime horaFin, String sala, String ponente, String tipo, String origen) {

        static RespuestaSesion de(Sesion s) {
            return new RespuestaSesion(s.id(), s.titulo(), s.descripcion(), s.fecha(), s.horaInicio(),
                    s.horaFin(), s.sala(), s.ponente(), s.tipo().name(), s.origen().name());
        }
    }
}
