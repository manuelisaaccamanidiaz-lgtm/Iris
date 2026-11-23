package com.iris.infrastructure.adapter.in.web.invitacion;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.invitacion.CancelarInvitacion;
import com.iris.application.invitacion.InvitarAlEvento;
import com.iris.application.invitacion.ListarInvitacionesDelEvento;
import com.iris.domain.evento.RolEnEvento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Lado del PROPIETARIO: invitar, ver y cancelar las invitaciones de un evento. */
@RestController
@RequestMapping("/api/eventos/{eventoId}/invitaciones")
class InvitacionEventoController {

    private final InvitarAlEvento invitar;
    private final ListarInvitacionesDelEvento listar;
    private final CancelarInvitacion cancelar;

    InvitacionEventoController(InvitarAlEvento invitar, ListarInvitacionesDelEvento listar,
                               CancelarInvitacion cancelar) {
        this.invitar = invitar;
        this.listar = listar;
        this.cancelar = cancelar;
    }

    /** {"email": "carla@mail.com", "rol": "EDITOR"} */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaInvitacion invitar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                                @Valid @RequestBody SolicitudInvitacion s) {
        return RespuestaInvitacion.de(invitar.ejecutar(usuario(jwt), eventoId, s.email(), s.rol()));
    }

    @GetMapping
    List<RespuestaInvitacion> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId) {
        return listar.ejecutar(usuario(jwt), eventoId).stream().map(RespuestaInvitacion::de).toList();
    }

    /** Cancelar no borra: la invitación queda CANCELADA (trazabilidad). */
    @DeleteMapping("/{invitacionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId, @PathVariable UUID invitacionId) {
        cancelar.ejecutar(usuario(jwt), eventoId, invitacionId);
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    record SolicitudInvitacion(@NotBlank String email, @NotNull RolEnEvento rol) {
    }
}
