package com.iris.application.documento;

import java.util.List;
import java.util.UUID;

import com.iris.application.port.out.AlmacenTemporal;
import com.iris.application.port.out.ExtractorTexto;
import com.iris.application.port.out.ExtractorTexto.PaginaTexto;
import com.iris.domain.documento.Documento;
import com.iris.domain.documento.DocumentoRepositorio;
import com.iris.domain.documento.EstadoDocumento;

/**
 * Caso de uso: indexar un documento (pasos 3 a 7 del flujo del profe). Corre en SEGUNDO PLANO.
 *
 * <p>Regla de oro del asíncrono: aquí NADIE de afuera va a atrapar una excepción
 * (el controlador ya respondió 202). Por eso todo va dentro de un try/catch y,
 * si algo falla, el documento queda en ERROR con un mensaje que el usuario puede ver.
 *
 * <p>Es repetible (idempotente): si la app se reinicia a mitad de camino, se puede volver
 * a ejecutar con el mismo documento sin dañar nada.
 */
public class ProcesarDocumento {

    private final DocumentoRepositorio documentos;
    private final AlmacenTemporal almacen;
    private final ExtractorTexto extractor;

    public ProcesarDocumento(DocumentoRepositorio documentos, AlmacenTemporal almacen,
                             ExtractorTexto extractor) {
        this.documentos = documentos;
        this.almacen = almacen;
        this.extractor = extractor;
    }

    public void ejecutar(UUID documentoId) {
        Documento doc = documentos.buscarParaProcesar(documentoId).orElse(null);
        if (doc == null || doc.estado() == EstadoDocumento.INDEXADO || doc.estado() == EstadoDocumento.ERROR) {
            return; // lo borraron, ya estaba listo o ya falló: nada que hacer
        }

        doc = documentos.guardar(doc.marcarProcesando());
        try {
            // Paso 3: extraer el texto página por página
            List<PaginaTexto> paginas = extractor.extraer(almacen.leer(doc.rutaTemporal()));
            boolean sinTexto = paginas.stream().allMatch(p -> p.texto().isBlank());
            if (sinTexto) {
                // Casi seguro un PDF escaneado (imágenes): necesitaría OCR (fuera del alcance por ahora)
                throw new IllegalStateException("El PDF no tiene texto seleccionable (¿es escaneado?)");
            }
            doc = documentos.guardar(doc.conPaginas(paginas.size()));

            // Pasos 4 a 7 (fragmentos, embeddings, guardar, borrar copia): llegan en los pasos 2.5 y 2.6.
            // Por ahora el documento se queda en PROCESANDO con su número de páginas.

        } catch (Exception e) {
            documentos.guardar(doc.marcarError(mensajeLegible(e)));
            almacen.borrar(doc.rutaTemporal()); // con ERROR no se reintenta: liberamos el disco
        }
    }

    private static String mensajeLegible(Exception e) {
        String m = e.getMessage();
        return (m == null || m.isBlank()) ? "Error inesperado al procesar el documento" : m;
    }
}
