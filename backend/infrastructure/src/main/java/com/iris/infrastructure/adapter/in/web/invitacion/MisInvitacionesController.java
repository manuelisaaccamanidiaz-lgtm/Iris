package com.iris.infrastructure.adapter.in.web.invitacion;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.invitacion.AceptarInvitacion;
import com.iris.application.invitacion.MisInvitaciones;
import com.iris.application.invitacion.RechazarInvitacion;

/**
 * Lado del INVITADO: su bandeja y sus respuestas. No va bajo /api/eventos/{id} porque
 * el invitado todavía NO es miembro: la regla aquí es "va dirigida a mi email".
 */
@RestController
@RequestMapping("/api/invitaciones")
class MisInvitacionesController {

    private final MisInvitaciones bandeja;
    private final AceptarInvitacion aceptar;
    private final RechazarInvitacion rechazar;

    MisInvitacionesController(MisInvitaciones bandeja, AceptarInvitacion aceptar, RechazarInvitacion rechazar) {
        this.bandeja = bandeja;
        this.aceptar = aceptar;
        this.rechazar = rechazar;
    }

    @GetMapping
    List<RespuestaInvitacion> bandeja(@AuthenticationPrincipal Jwt jwt) {
        return bandeja.ejecutar(usuario(jwt)).stream().map(RespuestaInvitacion::de).toList();
    }

    /** POST y no PATCH: "aceptar" es una ACCIÓN con efectos (te vuelve miembro), no editar un campo. */
    @PostMapping("/{invitacionId}/aceptar")
    RespuestaInvitacion aceptar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invitacionId) {
        return RespuestaInvitacion.de(aceptar.ejecutar(usuario(jwt), invitacionId));
    }

    @PostMapping("/{invitacionId}/rechazar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void rechazar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invitacionId) {
        rechazar.ejecutar(usuario(jwt), invitacionId);
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
