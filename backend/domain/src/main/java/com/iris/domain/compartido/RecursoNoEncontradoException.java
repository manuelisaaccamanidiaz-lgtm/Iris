package com.iris.domain.compartido;

/**
 * Se lanza cuando algo no existe... o no es tuyo. A propósito NO distinguimos:
 * si el evento existe pero es de otro usuario, respondemos igual que si no existiera (404).
 * Así nadie puede "tantear" ids ajenos.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
