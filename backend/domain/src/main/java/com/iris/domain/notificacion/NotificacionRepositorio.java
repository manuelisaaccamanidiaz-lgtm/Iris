package com.iris.domain.notificacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Puerto de salida para la campanita. Siempre las notificaciones DE una persona. */
public interface NotificacionRepositorio {

    Notificacion guardar(Notificacion notificacion);

    /** Las más recientes primero. */
    List<Notificacion> listarDe(UUID usuarioId, boolean soloNoLeidas, int limite);

    /** El numerito rojo. */
    int contarNoLeidas(UUID usuarioId);

    /** false si no existe O si no es de ese usuario (nadie marca como leídas las de otro). */
    boolean marcarLeida(UUID notificacionId, UUID usuarioId, Instant cuando);

    /** Devuelve cuántas se marcaron. */
    int marcarTodasLeidas(UUID usuarioId, Instant cuando);
}
