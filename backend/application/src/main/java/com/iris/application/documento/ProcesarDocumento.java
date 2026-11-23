package com.iris.application.documento;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.iris.application.documento.Fragmentador.Pedazo;
import com.iris.application.notificacion.Avisos;
import com.iris.application.port.out.AlmacenTemporal;
import com.iris.domain.notificacion.TipoNotificacion;
import com.iris.application.port.out.ExtractorTexto;
import com.iris.application.port.out.ExtractorTexto.PaginaTexto;
import com.iris.application.port.out.GeneradorEmbeddings;
import com.iris.domain.documento.Documento;
import com.iris.domain.documento.DocumentoRepositorio;
import com.iris.domain.documento.EstadoDocumento;
import com.iris.domain.documento.Fragmento;
import com.iris.domain.documento.FragmentoRepositorio;

/**
 * Caso de uso: indexar un documento (pasos 3 a 7 del flujo del profe). Corre en SEGUNDO PLANO.
 *
 * <p>Regla de oro del asíncrono: aquí NADIE de afuera va a atrapar una excepción
 * (el controlador ya respondió 202). Por eso todo va dentro de un try/catch y,
 * si algo falla, el documento queda en ERROR con un mensaje que el usuario puede ver.
 *
 * <p>Es repetible (idempotente): si la app se reinicia a mitad de camino, se puede volver
 * a ejecutar con el mismo documento sin duplicar fragmentos.
 */
public class ProcesarDocumento {

    private final DocumentoRepositorio documentos;
    private final FragmentoRepositorio fragmentos;
    private final AlmacenTemporal almacen;
    private final ExtractorTexto extractor;
    private final Fragmentador fragmentador;
    private final GeneradorEmbeddings embeddings;
    private final Avisos avisos;

    public ProcesarDocumento(DocumentoRepositorio documentos, FragmentoRepositorio fragmentos,
                             AlmacenTemporal almacen, ExtractorTexto extractor,
                             Fragmentador fragmentador, GeneradorEmbeddings embeddings, Avisos avisos) {
        this.documentos = documentos;
        this.fragmentos = fragmentos;
        this.almacen = almacen;
        this.extractor = extractor;
        this.fragmentador = fragmentador;
        this.embeddings = embeddings;
        this.avisos = avisos;
    }

    /** "Documento indexado" a quien lo subió y a los propietarios (sin repetirle a quien lo subió). */
    private void avisarIndexado(Documento doc) {
        String mensaje = "El documento «" + doc.nombreArchivo() + "» ya está listo: puedes preguntarle a Iris";
        if (doc.subidoPor() != null) {
            avisos.aMiembro(doc.eventoId(), doc.subidoPor(), TipoNotificacion.DOCUMENTO_INDEXADO, mensaje, doc.id());
        }
        avisos.aPropietarios(doc.eventoId(), TipoNotificacion.DOCUMENTO_INDEXADO, mensaje, doc.id(), doc.subidoPor());
    }

    public void ejecutar(UUID documentoId) {
        Documento doc = documentos.buscarParaProcesar(documentoId).orElse(null);
        if (doc == null || doc.estado() == EstadoDocumento.INDEXADO || doc.estado() == EstadoDocumento.ERROR) {
            return; // lo borraron, ya estaba listo o ya falló: nada que hacer
        }

        doc = documentos.guardar(doc.marcarProcesando());
        String ruta = doc.rutaTemporal();
        try {
            // Paso 3: extraer el texto página por página
            List<PaginaTexto> paginas = extractor.extraer(almacen.leer(ruta));
            if (paginas.stream().allMatch(p -> p.texto().isBlank())) {
                // Casi seguro un PDF escaneado (imágenes): necesitaría OCR (fuera del alcance por ahora)
                throw new IllegalStateException("El PDF no tiene texto seleccionable (¿es escaneado?)");
            }
            doc = documentos.guardar(doc.conPaginas(paginas.size()));

            // Paso 4: fragmentos de 500-1000 tokens con solapamiento, conservando páginas y sección
            List<Pedazo> pedazos = fragmentador.fragmentar(paginas);

            // Paso 5: un embedding por fragmento (en el MISMO orden que los textos)
            List<float[]> vectores = embeddings.paraDocumentos(pedazos.stream().map(Pedazo::contenido).toList());

            // Paso 6: guardar texto + vector + evento + metadatos (borra antes los de un intento previo)
            List<Fragmento> nuevos = new ArrayList<>(pedazos.size());
            for (int i = 0; i < pedazos.size(); i++) {
                Pedazo p = pedazos.get(i);
                nuevos.add(new Fragmento(doc.id(), doc.eventoId(), i, p.paginaInicio(), p.paginaFin(),
                        p.seccion(), p.contenido(), p.tokens(), vectores.get(i)));
            }
            fragmentos.reemplazarDelDocumento(doc.id(), nuevos);

            // Paso 7: verificar que todo quedó guardado y SOLO entonces borrar la copia temporal
            int guardados = fragmentos.contarPorDocumento(doc.id());
            if (guardados != nuevos.size()) {
                throw new IllegalStateException("Se esperaban " + nuevos.size() + " fragmentos y hay " + guardados);
            }
            almacen.borrar(ruta);
            documentos.guardar(doc.marcarIndexado(paginas.size(), embeddings.modelo(), embeddings.dimensiones()));
            avisarIndexado(doc);

        } catch (Exception e) {
            documentos.guardar(doc.marcarError(mensajeLegible(e)));
            almacen.borrar(ruta); // con ERROR no se reintenta: liberamos el disco
        }
    }

    private static String mensajeLegible(Exception e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) {
            return "Error inesperado al procesar el documento";
        }
        return m.length() > 500 ? m.substring(0, 500) : m;
    }
}
