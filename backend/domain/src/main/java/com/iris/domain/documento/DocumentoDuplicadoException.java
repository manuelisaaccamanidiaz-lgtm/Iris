package com.iris.domain.documento;

import com.iris.domain.compartido.ReglaDeNegocioException;

/** El mismo PDF (misma huella SHA-256) ya está en este evento. */
public class DocumentoDuplicadoException extends ReglaDeNegocioException {

    public DocumentoDuplicadoException(String nombreExistente) {
        super("Este PDF ya fue subido al evento (como \"" + nombreExistente + "\")");
    }
}
