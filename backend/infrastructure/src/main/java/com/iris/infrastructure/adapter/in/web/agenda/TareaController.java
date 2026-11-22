package com.iris.infrastructure.adapter.in.web.agenda;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.agenda.CambiarEstadoTarea;
import com.iris.application.agenda.CrearTarea;
import com.iris.application.agenda.EditarTarea;
import com.iris.application.agenda.EliminarTarea;
import com.iris.application.agenda.ListarTareas;
import com.iris.domain.agenda.EstadoTarea;
import com.iris.domain.agenda.Tarea;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Las tareas del evento. Filtros opcionales: ?estado=PENDIENTE y ?mias=true */
@RestController
@RequestMapping("/api/eventos/{eventoId}/tareas")
class TareaController {

    private final CrearTarea crear;
    private final ListarTareas listar;
    private final EditarTarea editar;
    private final CambiarEstadoTarea cambiarEstado;
    private final EliminarTarea eliminar;

    TareaController(CrearTarea crear, ListarTareas listar, EditarTarea editar,
                    CambiarEstadoTarea cambiarEstado, EliminarTarea eliminar) {
        this.crear = crear;
        this.listar = listar;
        this.editar = editar;
        this.cambiarEstado = cambiarEstado;
        this.eliminar = eliminar;
    }

    @GetMapping
    List<RespuestaTarea> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                                @RequestParam(required = false) EstadoTarea estado,
                                @RequestParam(defaultValue = "false") boolean mias) {
        return listar.ejecutar(usuario(jwt), eventoId, estado, mias).stream().map(RespuestaTarea::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaTarea crear(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                         @Valid @RequestBody SolicitudTarea s) {
        return RespuestaTarea.de(crear.ejecutar(usuario(jwt), eventoId, s.aDatos()));
    }

    @PutMapping("/{tareaId}")
    RespuestaTarea editar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                          @PathVariable UUID tareaId, @Valid @RequestBody SolicitudTarea s) {
        return RespuestaTarea.de(editar.ejecutar(usuario(jwt), eventoId, tareaId, s.aDatos()));
    }

    /** PATCH = cambio PARCIAL: solo el estado. {"estado": "HECHA"} o {"estado": "PENDIENTE"} */
    @PatchMapping("/{tareaId}/estado")
    RespuestaTarea cambiarEstado(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                                 @PathVariable UUID tareaId, @Valid @RequestBody SolicitudEstado s) {
        return RespuestaTarea.de(cambiarEstado.ejecutar(usuario(jwt), eventoId, tareaId, s.estado()));
    }

    @DeleteMapping("/{tareaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId, @PathVariable UUID tareaId) {
        eliminar.ejecutar(usuario(jwt), eventoId, tareaId);
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    record SolicitudTarea(@NotBlank String titulo, String descripcion, LocalDate fechaLimite, UUID responsableId) {

        Tarea.Datos aDatos() {
            return new Tarea.Datos(titulo, descripcion, fechaLimite, responsableId);
        }
    }

    record SolicitudEstado(@NotNull EstadoTarea estado) {
    }

    record RespuestaTarea(UUID id, String titulo, String descripcion, LocalDate fechaLimite, String estado,
                          UUID responsableId, String origen, Instant completadaEn) {

        static RespuestaTarea de(Tarea t) {
            return new RespuestaTarea(t.id(), t.titulo(), t.descripcion(), t.fechaLimite(), t.estado().name(),
                    t.responsableId(), t.origen().name(), t.completadaEn());
        }
    }
}
