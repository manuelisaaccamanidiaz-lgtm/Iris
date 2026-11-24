package com.iris.domain.evento;

import java.time.Instant;
import java.util.UUID;

import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Una persona del equipo de un evento, con su rol. Es lo que muestra "ver el equipo".
 *
 * @param archivadoEn si no es null, la persona se "salió" de un evento terminado:
 *                    sigue registrada (trazabilidad), pero el evento ya no le aparece en su lista
 */
public record MiembroEvento(
        UUID eventoId,
        UUID usuarioId,
        String nombre,
        String email,
        RolEnEvento rol,
        Instant unidoEn,
        Instant archivadoEn) {

    public MiembroEvento {
        if (eventoId == null || usuarioId == null || rol == null || unidoEn == null) {
            throw new ReglaDeNegocioException("Faltan datos obligatorios del miembro");
        }
    }
}
