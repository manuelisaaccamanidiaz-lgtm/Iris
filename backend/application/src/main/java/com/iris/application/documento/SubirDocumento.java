package com.iris.application.documento;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.iris.application.port.out.AlmacenTemporal;
import com.iris.application.port.out.Antivirus;
import com.iris.application.port.out.ColaIndexacion;
import com.iris.application.port.out.InspectorPdf;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.documento.ArchivoPdf;
import com.iris.domain.documento.ArchivoRechazadoException;
import com.iris.domain.documento.Documento;
import com.iris.domain.documento.DocumentoDuplicadoException;
import com.iris.domain.documento.DocumentoRepositorio;
import com.iris.domain.documento.EstadoDocumento;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.EventoRepositorio;

/**
 * Caso de uso: un EDITOR o PROPIETARIO sube un PDF a un evento (pasos 1 y 2 del flujo del profe).
 *
 * <p>Requisito: "límites de tamaño, validación y escaneo de archivos". El orden de las revisiones importa:
 * <pre>
 * tamaño -> ANTIVIRUS -> ¿es un PDF? -> ESTRUCTURA del PDF -> duplicado -> copia temporal -> cola
 * </pre>
 * El antivirus va ANTES de abrir el PDF con PDFBox: abrir (parsear) un archivo malicioso ya es un riesgo,
 * así que primero lo mira el antivirus "desde afuera", como bytes.
 */
public class SubirDocumento {

    private final EventoRepositorio eventos;
    private final DocumentoRepositorio documentos;
    private final AlmacenTemporal almacen;
    private final ColaIndexacion cola;
    private final Antivirus antivirus;
    private final InspectorPdf inspector;
    private final long tamanoMaximoBytes;

    public SubirDocumento(EventoRepositorio eventos, DocumentoRepositorio documentos,
                          AlmacenTemporal almacen, ColaIndexacion cola, Antivirus antivirus,
                          InspectorPdf inspector, long tamanoMaximoBytes) {
        this.eventos = eventos;
        this.documentos = documentos;
        this.almacen = almacen;
        this.cola = cola;
        this.antivirus = antivirus;
        this.inspector = inspector;
        this.tamanoMaximoBytes = tamanoMaximoBytes;
    }

    public Documento ejecutar(UUID usuarioId, UUID eventoId, String nombreArchivo, byte[] contenido) {
        // 1. ¿Es miembro? (si no: 404)  ¿Su rol permite subir? (si no: 403)
        EventoDeMiembro evento = eventos.buscarComoMiembro(eventoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
        evento.exigirEdicion();

        // 2. Tamaño razonable (Spring ya corta antes de leerlo, pero el caso de uso no depende de eso)
        if (contenido != null && contenido.length > tamanoMaximoBytes) {
            throw new ReglaDeNegocioException("El PDF supera el tamaño máximo de "
                    + (tamanoMaximoBytes / (1024 * 1024)) + " MB");
        }
        // 2b. ESCANEO: el antivirus mira los bytes ANTES de que nadie abra el archivo
        if (contenido != null) {
            antivirus.escanear(contenido).ifPresent(amenaza -> {
                throw new ArchivoRechazadoException("el antivirus detectó «" + amenaza + "»");
            });
        }
        // 2c. ¿Es un PDF real? (nombre y "bytes mágicos" %PDF-)
        ArchivoPdf archivo = new ArchivoPdf(nombreArchivo, contenido);
        // 2d. VALIDACIÓN de la estructura: nada de contenido activo
        List<String> riesgos = inspector.riesgos(contenido);
        if (!riesgos.isEmpty()) {
            throw new ArchivoRechazadoException("el PDF trae " + String.join(", ", riesgos)
                    + ". Un documento del evento no necesita eso");
        }

        // 3. ¿Ya lo habían subido a este evento? Comparamos huellas, no nombres
        String hash = sha256(contenido);
        //    Excepción: si el anterior terminó en ERROR, dejamos subirlo otra vez (reemplaza al fallido)
        documentos.buscarPorEventoYHash(eventoId, hash).ifPresent(existente -> {
            if (existente.estado() != EstadoDocumento.ERROR) {
                throw new DocumentoDuplicadoException(existente.nombreArchivo());
            }
            documentos.eliminar(existente.id());
        });

        // 4. Copia temporal primero, registro después. Si el registro falla,
        //    borramos la copia para no dejar archivos huérfanos en el disco
        String ruta = almacen.guardar(contenido);
        Documento guardado;
        try {
            guardado = documentos.guardar(Documento.recibir(eventoId, usuarioId, archivo, hash, ruta));
        } catch (RuntimeException e) {
            almacen.borrar(ruta);
            throw e;
        }

        // 5. A la fila. encolar() regresa al instante: el controlador responde 202 sin esperar
        cola.encolar(guardado.id());
        return guardado;
    }

    /** Huella SHA-256 en hexadecimal (64 caracteres). Si cambia 1 byte del PDF, cambia toda la huella. */
    private static String sha256(byte[] contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e); // nunca pasa en Java estándar
        }
    }
}
