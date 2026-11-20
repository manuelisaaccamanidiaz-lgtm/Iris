package com.iris.application.documento;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import com.iris.application.port.out.AlmacenTemporal;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.documento.ArchivoPdf;
import com.iris.domain.documento.Documento;
import com.iris.domain.documento.DocumentoDuplicadoException;
import com.iris.domain.documento.DocumentoRepositorio;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.EventoRepositorio;

/** Caso de uso: un EDITOR o PROPIETARIO sube un PDF a un evento (pasos 1 y 2 del flujo del profe). */
public class SubirDocumento {

    private final EventoRepositorio eventos;
    private final DocumentoRepositorio documentos;
    private final AlmacenTemporal almacen;
    private final long tamanoMaximoBytes;

    public SubirDocumento(EventoRepositorio eventos, DocumentoRepositorio documentos,
                          AlmacenTemporal almacen, long tamanoMaximoBytes) {
        this.eventos = eventos;
        this.documentos = documentos;
        this.almacen = almacen;
        this.tamanoMaximoBytes = tamanoMaximoBytes;
    }

    public Documento ejecutar(UUID usuarioId, UUID eventoId, String nombreArchivo, byte[] contenido) {
        // 1. ¿Es miembro? (si no: 404)  ¿Su rol permite subir? (si no: 403)
        EventoDeMiembro evento = eventos.buscarComoMiembro(eventoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
        evento.exigirEdicion();

        // 2. ¿Es un PDF real y de tamaño razonable?
        ArchivoPdf archivo = new ArchivoPdf(nombreArchivo, contenido);
        if (archivo.tamanoBytes() > tamanoMaximoBytes) {
            throw new ReglaDeNegocioException("El PDF supera el tamaño máximo de "
                    + (tamanoMaximoBytes / (1024 * 1024)) + " MB");
        }

        // 3. ¿Ya lo habían subido a este evento? Comparamos huellas, no nombres
        String hash = sha256(contenido);
        documentos.buscarPorEventoYHash(eventoId, hash).ifPresent(existente -> {
            throw new DocumentoDuplicadoException(existente.nombreArchivo());
        });

        // 4. Copia temporal primero, registro después. Si el registro falla,
        //    borramos la copia para no dejar archivos huérfanos en el disco
        String ruta = almacen.guardar(contenido);
        try {
            return documentos.guardar(Documento.recibir(eventoId, usuarioId, archivo, hash, ruta));
        } catch (RuntimeException e) {
            almacen.borrar(ruta);
            throw e;
        }
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
