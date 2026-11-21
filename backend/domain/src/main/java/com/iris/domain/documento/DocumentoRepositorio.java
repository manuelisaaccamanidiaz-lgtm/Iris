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

    /**
     * Búsqueda por id SIN filtro de evento. Solo para el procesador interno (no hay usuario
     * detrás); NUNCA se usa desde un controlador web.
     */
    Optional<Documento> buscarParaProcesar(UUID id);

    /** Ids de documentos que quedaron EN_COLA o PROCESANDO (usa el índice parcial de la V4). */
    List<UUID> idsPendientes();

    void eliminar(UUID id);

    /** Si ya existe un documento con esa huella en el evento, lo devuelve. */
    Optional<Documento> buscarPorEventoYHash(UUID eventoId, String hashSha256);

    /** Los más recientes primero (para el panel derecho). */
    List<Documento> listarPorEvento(UUID eventoId);
}
