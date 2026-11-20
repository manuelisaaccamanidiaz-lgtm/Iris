package com.iris.domain.evento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para eventos. Fíjate: NO existe "buscarPorId(id)" a secas.
 * Toda búsqueda exige el usuarioId: el multi-tenant está en la firma del método.
 */
public interface EventoRepositorio {

    Evento guardar(Evento evento);

    /** Eventos de un usuario, del más próximo al más lejano. */
    List<Evento> listarPorUsuario(UUID usuarioId);

    /** Vacío si no existe O si es de otro usuario. */
    Optional<Evento> buscarPorIdYUsuario(UUID id, UUID usuarioId);
}
