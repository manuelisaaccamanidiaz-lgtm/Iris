package com.iris.domain.compartido;

/**
 * Se lanza cuando algo viola una regla del negocio (email inválido, nombre vacío...).
 * Es Java puro: más adelante la capa web la traduce a un HTTP 400.
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
