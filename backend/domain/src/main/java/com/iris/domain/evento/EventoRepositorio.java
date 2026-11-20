package com.iris.domain.evento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para eventos. Sigue sin existir "buscarPorId(id)" a secas:
 * toda búsqueda se hace COMO miembro, y si no lo eres, el evento "no existe" para ti.
 */
public interface EventoRepositorio {

    /** Guarda el evento Y registra a su creador como PROPIETARIO, todo o nada (una transacción). */
    EventoDeMiembro crearConPropietario(Evento evento);

    /** Eventos donde el usuario es miembro (cualquier rol), del más próximo al más lejano. */
    List<EventoDeMiembro> listarDeMiembro(UUID usuarioId);

    /** Vacío si el evento no existe O si el usuario no es miembro. */
    Optional<EventoDeMiembro> buscarComoMiembro(UUID eventoId, UUID usuarioId);
}
