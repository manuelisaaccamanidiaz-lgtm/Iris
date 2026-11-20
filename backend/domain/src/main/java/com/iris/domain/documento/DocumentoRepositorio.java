package com.iris.domain.documento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para documentos. Todas las búsquedas son POR EVENTO:
 * quien llama ya verificó antes que el usuario es miembro de ese evento.
 */
public interface DocumentoRepositorio {

    Documento guardar(Documento documento);

    /** Si ya existe un documento con esa huella en el evento, lo devuelve. */
    Optional<Documento> buscarPorEventoYHash(UUID eventoId, String hashSha256);

    /** Los más recientes primero (para el panel derecho). */
    List<Documento> listarPorEvento(UUID eventoId);
}
