package com.iris.application.port.out;

import java.util.UUID;

import com.iris.domain.notificacion.TipoNotificacion;

/**
 * Puerto de salida: "avisa a esta persona". Los casos de uso NO saben si el aviso termina
 * en la campanita, en un correo de Gmail o en Mailpit: eso lo decide infrastructure.
 */
public interface Notificador {

    void notificar(Aviso aviso);

    /**
     * @param usuarioId null si la persona aún no tiene cuenta (una invitación a un email nuevo):
     *                  entonces no hay campanita, solo correo
     * @param email     a dónde mandar el correo (si el tipo lo merece)
     */
    record Aviso(UUID usuarioId, String email, UUID eventoId, TipoNotificacion tipo,
                 String mensaje, UUID referenciaId) {
    }
}
