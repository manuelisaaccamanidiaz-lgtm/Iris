package com.iris.domain.compartido;

/**
 * Eres miembro del evento (sabes que existe), pero tu rol no permite esta acción -> HTTP 403.
 * Si NO eres miembro, usamos RecursoNoEncontradoException (404) para no revelar que existe.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
