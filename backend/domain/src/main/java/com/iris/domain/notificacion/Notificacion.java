package com.iris.domain.notificacion;

import java.time.Instant;
import java.util.UUID;

import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Un aviso para UNA persona (lo que aparece en la campanita).
 *
 * @param referenciaId a qué apunta (tarea, sesión, documento o invitación); puede ser null
 * @param leidaEn      null mientras no se ha leído
 */
public record Notificacion(
        UUID id,
        UUID usuarioId,
        UUID eventoId,
        TipoNotificacion tipo,
        String mensaje,
        UUID referenciaId,
        Instant creadaEn,
        Instant leidaEn) {

    /** Igual que el VARCHAR(300) de la V11. */
    public static final int MAX_MENSAJE = 300;

    public Notificacion {
        if (usuarioId == null || eventoId == null || tipo == null || creadaEn == null) {
            throw new ReglaDeNegocioException("Faltan datos obligatorios de la notificación");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new ReglaDeNegocioException("La notificación necesita un mensaje");
        }
        mensaje = mensaje.trim();
        // Un título de tarea muy largo no debe romper el aviso: lo recortamos con "..."
        if (mensaje.length() > MAX_MENSAJE) {
            mensaje = mensaje.substring(0, MAX_MENSAJE - 3) + "...";
        }
    }

    public static Notificacion crear(UUID usuarioId, UUID eventoId, TipoNotificacion tipo,
                                     String mensaje, UUID referenciaId) {
        return new Notificacion(null, usuarioId, eventoId, tipo, mensaje, referenciaId, Instant.now(), null);
    }

    public boolean leida() {
        return leidaEn != null;
    }
}
