package com.iris.domain.invitacion;

/**
 * Ciclo de vida de una invitación. Solo se sale de PENDIENTE, nunca se vuelve:
 *
 * <pre>
 *             ┌──► ACEPTADA   (el invitado entra al evento)
 *             ├──► RECHAZADA  (lo decide el invitado)
 * PENDIENTE ──┼──► CANCELADA  (lo decide un propietario)
 *             └──► VENCIDA    (pasaron 7 días sin respuesta)
 * </pre>
 */
public enum EstadoInvitacion {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    CANCELADA,
    VENCIDA
}
