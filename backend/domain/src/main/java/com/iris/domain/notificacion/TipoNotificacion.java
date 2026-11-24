package com.iris.domain.notificacion;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Qué pasó. Cada tipo "sabe" si además merece un CORREO (decisión 4A de Isaac:
 * solo la invitación y los recordatorios; la campanita recibe todo siempre).
 */
public enum TipoNotificacion {

    INVITACION_RECIBIDA(true),
    MIEMBRO_NUEVO(false),
    SESION_NUEVA(false),
    TAREA_NUEVA(false),
    TAREA_ASIGNADA(false),
    DOCUMENTO_INDEXADO(false),
    PROPUESTAS_NUEVAS(false),
    TAREA_VENCE_MANANA(true),
    TAREA_VENCE_HOY(true),
    TAREA_VENCIDA(true);

    private final boolean enviaCorreo;

    TipoNotificacion(boolean enviaCorreo) {
        this.enviaCorreo = enviaCorreo;
    }

    public boolean enviaCorreo() {
        return enviaCorreo;
    }

    /**
     * ¿Qué recordatorio le toca HOY a una tarea pendiente con esta fecha límite? (decisión 1B)
     * <ul>
     *   <li>vence mañana -> TAREA_VENCE_MANANA</li>
     *   <li>vence hoy    -> TAREA_VENCE_HOY</li>
     *   <li>ya venció    -> TAREA_VENCIDA (una sola vez: el registro de enviados evita repetirlo)</li>
     *   <li>más adelante -> ninguno todavía</li>
     * </ul>
     */
    public static Optional<TipoNotificacion> recordatorioPara(LocalDate fechaLimite, LocalDate hoy) {
        if (fechaLimite == null) {
            return Optional.empty();
        }
        if (fechaLimite.isBefore(hoy)) {
            return Optional.of(TAREA_VENCIDA);
        }
        if (fechaLimite.isEqual(hoy)) {
            return Optional.of(TAREA_VENCE_HOY);
        }
        if (fechaLimite.isEqual(hoy.plusDays(1))) {
            return Optional.of(TAREA_VENCE_MANANA);
        }
        return Optional.empty();
    }
}
