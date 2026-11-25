package com.iris.domain.documento;

import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * El archivo NO se acepta por seguridad: el antivirus encontró una amenaza, o el PDF trae contenido
 * activo (JavaScript, archivos incrustados...). Hereda de ReglaDeNegocioException: responde 400 con
 * el motivo, para que la persona sepa POR QUÉ se rechazó su archivo.
 */
public class ArchivoRechazadoException extends ReglaDeNegocioException {

    public ArchivoRechazadoException(String motivo) {
        super("No se aceptó el archivo por seguridad: " + motivo);
    }
}
