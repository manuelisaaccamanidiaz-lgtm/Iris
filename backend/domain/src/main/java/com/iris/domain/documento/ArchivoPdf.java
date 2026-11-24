package com.iris.domain.documento;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Objeto de valor: un archivo que, si existe, ES un PDF de verdad.
 *
 * <p>No confiamos en la extensión ".pdf" ni en el Content-Type que manda el navegador
 * (ambos se falsifican en un segundo). Revisamos los "bytes mágicos": todo PDF real
 * empieza con los caracteres %PDF-
 */
public record ArchivoPdf(String nombre, byte[] contenido) {

    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public ArchivoPdf {
        nombre = limpiarNombre(nombre);
        if (!nombre.toLowerCase().endsWith(".pdf")) {
            throw new ReglaDeNegocioException("Solo se aceptan archivos .pdf");
        }
        if (contenido == null || contenido.length == 0) {
            throw new ReglaDeNegocioException("El archivo está vacío");
        }
        if (contenido.length < FIRMA_PDF.length
                || !Arrays.equals(Arrays.copyOf(contenido, FIRMA_PDF.length), FIRMA_PDF)) {
            throw new ReglaDeNegocioException("El archivo no es un PDF válido");
        }
    }

    public long tamanoBytes() {
        return contenido.length;
    }

    /**
     * Quita rutas ("C:\\Users\\..\\contrato.pdf" o "../../etc/x.pdf") y deja solo el nombre.
     * Evita ataques de "path traversal" y nombres absurdamente largos.
     */
    private static String limpiarNombre(String original) {
        if (original == null || original.isBlank()) {
            throw new ReglaDeNegocioException("El archivo no tiene nombre");
        }
        String limpio = original.replace('\\', '/');
        limpio = limpio.substring(limpio.lastIndexOf('/') + 1).trim();
        if (limpio.isEmpty()) {
            throw new ReglaDeNegocioException("El archivo no tiene nombre");
        }
        return limpio.length() > 255 ? limpio.substring(limpio.length() - 255) : limpio;
    }
}
