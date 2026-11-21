package com.iris.domain.documento;

import java.time.Instant;
import java.util.UUID;

import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Un PDF subido a un evento. OJO: no contiene el PDF, solo sus metadatos.
 * Es inmutable: cada cambio de estado devuelve un Documento NUEVO.
 *
 * @param subidoPor   puede ser null si esa persona borró su cuenta (el documento se queda)
 * @param rutaTemporal copia temporal mientras se indexa; null cuando ya se borró
 */
public record Documento(
        UUID id,
        UUID eventoId,
        UUID subidoPor,
        String nombreArchivo,
        long tamanoBytes,
        String hashSha256,
        Integer paginas,
        EstadoDocumento estado,
        String errorMensaje,
        String modeloEmbedding,
        Integer dimensiones,
        String rutaTemporal,
        Instant creadoEn,
        Instant actualizadoEn,
        Instant indexadoEn) {

    public Documento {
        if (eventoId == null || nombreArchivo == null || hashSha256 == null || estado == null
                || creadoEn == null || actualizadoEn == null) {
            throw new ReglaDeNegocioException("Faltan datos obligatorios del documento");
        }
        if (tamanoBytes <= 0) {
            throw new ReglaDeNegocioException("El tamaño del documento debe ser mayor que cero");
        }
    }

    /** Un PDF recién recibido: queda EN_COLA esperando al procesador. */
    public static Documento recibir(UUID eventoId, UUID subidoPor, ArchivoPdf archivo,
                                    String hashSha256, String rutaTemporal) {
        Instant ahora = Instant.now();
        return new Documento(null, eventoId, subidoPor, archivo.nombre(), archivo.tamanoBytes(), hashSha256,
                null, EstadoDocumento.EN_COLA, null, null, null, rutaTemporal, ahora, ahora, null);
    }

    public Documento marcarProcesando() {
        return new Documento(id, eventoId, subidoPor, nombreArchivo, tamanoBytes, hashSha256, paginas,
                EstadoDocumento.PROCESANDO, null, modeloEmbedding, dimensiones, rutaTemporal,
                creadoEn, Instant.now(), indexadoEn);
    }

    /** Ya sabemos cuántas páginas tiene (después de extraer el texto). */
    public Documento conPaginas(int paginas) {
        return new Documento(id, eventoId, subidoPor, nombreArchivo, tamanoBytes, hashSha256, paginas,
                estado, errorMensaje, modeloEmbedding, dimensiones, rutaTemporal,
                creadoEn, Instant.now(), indexadoEn);
    }

    /** Indexado con éxito: guardamos trazabilidad (modelo, dimensiones) y ya no hay copia temporal. */
    public Documento marcarIndexado(int paginas, String modeloEmbedding, int dimensiones) {
        Instant ahora = Instant.now();
        return new Documento(id, eventoId, subidoPor, nombreArchivo, tamanoBytes, hashSha256, paginas,
                EstadoDocumento.INDEXADO, null, modeloEmbedding, dimensiones, null,
                creadoEn, ahora, ahora);
    }

    public Documento marcarError(String mensaje) {
        return new Documento(id, eventoId, subidoPor, nombreArchivo, tamanoBytes, hashSha256, paginas,
                EstadoDocumento.ERROR, mensaje, modeloEmbedding, dimensiones, null,
                creadoEn, Instant.now(), indexadoEn);
    }
}
