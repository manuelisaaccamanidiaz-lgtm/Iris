package com.iris.infrastructure.adapter.in.web.notificacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.notificacion.MisNotificaciones;
import com.iris.domain.notificacion.Notificacion;

/** La campanita: siempre las notificaciones de QUIEN pregunta (sale del JWT, nunca de la URL). */
@RestController
@RequestMapping("/api/notificaciones")
class NotificacionController {

    private final MisNotificaciones notificaciones;

    NotificacionController(MisNotificaciones notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping
    List<RespuestaNotificacion> listar(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(defaultValue = "false") boolean noLeidas) {
        return notificaciones.listar(usuario(jwt), noLeidas).stream().map(RespuestaNotificacion::de).toList();
    }

    /** El numerito rojo. React lo pedirá cada 30 s (decisión 3A). */
    @GetMapping("/conteo")
    Conteo conteo(@AuthenticationPrincipal Jwt jwt) {
        return new Conteo(notificaciones.contarNoLeidas(usuario(jwt)));
    }

    @PatchMapping("/{notificacionId}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void marcarLeida(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID notificacionId) {
        notificaciones.marcarLeida(usuario(jwt), notificacionId);
    }

    @PatchMapping("/leidas")
    Marcadas marcarTodas(@AuthenticationPrincipal Jwt jwt) {
        return new Marcadas(notificaciones.marcarTodasLeidas(usuario(jwt)));
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    record Conteo(int noLeidas) {
    }

    record Marcadas(int marcadas) {
    }

    record RespuestaNotificacion(UUID id, UUID eventoId, String tipo, String mensaje, UUID referenciaId,
                                 Instant creadaEn, boolean leida) {

        static RespuestaNotificacion de(Notificacion n) {
            return new RespuestaNotificacion(n.id(), n.eventoId(), n.tipo().name(), n.mensaje(),
                    n.referenciaId(), n.creadaEn(), n.leida());
        }
    }
}
