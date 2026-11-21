package com.iris.infrastructure.adapter.out.persistence.evento;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * La PK compuesta (evento_id, usuario_id) como objeto. JPA exige que una clave
 * compuesta sea Serializable y tenga equals/hashCode (para comparar claves).
 */
@Embeddable
public class MiembroEventoId implements Serializable {

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    protected MiembroEventoId() {
    }

    MiembroEventoId(UUID eventoId, UUID usuarioId) {
        this.eventoId = eventoId;
        this.usuarioId = usuarioId;
    }

    UUID getEventoId() { return eventoId; }
    UUID getUsuarioId() { return usuarioId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MiembroEventoId otro)) return false;
        return Objects.equals(eventoId, otro.eventoId) && Objects.equals(usuarioId, otro.usuarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventoId, usuarioId);
    }
}
