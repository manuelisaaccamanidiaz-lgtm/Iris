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

    /**
     * Borra el documento SOLO si no se está indexando, en una sola sentencia (DELETE ... WHERE estado <> ...).
     * Así no hay hueco entre "revisar el estado" y "borrar" en el que el indexador pueda empezar.
     * Los fragmentos se van en CASCADA; las citas del historial y las propuestas decididas quedan (SET NULL).
     * @return false si no se borró (ya no existía o empezó a indexarse justo en ese instante)
     */
    boolean eliminarSiNoSeEstaIndexando(UUID id);

    /** Si ya existe un documento con esa huella en el evento, lo devuelve. */
    Optional<Documento> buscarPorEventoYHash(UUID eventoId, String hashSha256);

    /** Los más recientes primero (para el panel derecho). */
    List<Documento> listarPorEvento(UUID eventoId);
}
