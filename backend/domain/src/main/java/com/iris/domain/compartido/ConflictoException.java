package com.iris.domain.compartido;

/**
 * La petición es válida, pero CHOCA con el estado actual de los datos
 * (ya es miembro, ya hay una invitación pendiente, la invitación ya fue respondida...).
 * Es Java puro: la capa web la traduce a un HTTP 409.
 */
public class ConflictoException extends ReglaDeNegocioException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
