package com.iris.domain.evento;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida para el equipo (tabla evento_miembros). */
public interface MiembroRepositorio {

    /** El equipo del evento: propietarios primero, luego por nombre. */
    List<MiembroEvento> listar(UUID eventoId);

    Optional<MiembroEvento> buscar(UUID eventoId, UUID usuarioId);

    /** ¿Alguien con ese email (normalizado) ya está en el equipo? */
    boolean existeConEmail(UUID eventoId, String email);

    void agregar(UUID eventoId, UUID usuarioId, RolEnEvento rol);

    void cambiarRol(UUID eventoId, UUID usuarioId, RolEnEvento nuevoRol);

    /** Borra la fila. Sus tareas quedan sin responsable gracias a la FK de la V9 (SET NULL). */
    void quitar(UUID eventoId, UUID usuarioId);

    /** No borra: marca archivado_en para que el evento deje de aparecerle (decisión 3c). */
    void archivar(UUID eventoId, UUID usuarioId, Instant cuando);
}
