package com.iris.application.notificacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.notificacion.Notificacion;
import com.iris.domain.notificacion.NotificacionRepositorio;

/**
 * Casos de uso de la campanita (los cuatro son de una línea, así que van juntos en una clase:
 * excepción consciente a "una clase = un ejecutar"). Siempre sobre las notificaciones DE quien pregunta.
 */
public class MisNotificaciones {

    /** Para que una lista nunca sea gigante: la campanita muestra las 50 más recientes. */
    private static final int LIMITE = 50;

    private final NotificacionRepositorio notificaciones;

    public MisNotificaciones(NotificacionRepositorio notificaciones) {
        this.notificaciones = notificaciones;
    }

    public List<Notificacion> listar(UUID yo, boolean soloNoLeidas) {
        return notificaciones.listarDe(yo, soloNoLeidas, LIMITE);
    }

    /** Lo que React preguntará cada 30 segundos (decisión 3A): barato a propósito. */
    public int contarNoLeidas(UUID yo) {
        return notificaciones.contarNoLeidas(yo);
    }

    public void marcarLeida(UUID yo, UUID notificacionId) {
        if (!notificaciones.marcarLeida(notificacionId, yo, Instant.now())) {
            throw new RecursoNoEncontradoException("Notificación no encontrada");
        }
    }

    public int marcarTodasLeidas(UUID yo) {
        return notificaciones.marcarTodasLeidas(yo, Instant.now());
    }
}
