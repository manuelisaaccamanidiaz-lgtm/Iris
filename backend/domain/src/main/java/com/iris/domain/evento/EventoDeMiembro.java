package com.iris.domain.evento;

import com.iris.domain.compartido.AccesoDenegadoException;

/**
 * Un evento visto POR un miembro: el evento + el rol que esa persona tiene en él.
 * Es lo que devuelve toda búsqueda: nunca obtienes un evento sin saber tu rol.
 */
public record EventoDeMiembro(Evento evento, RolEnEvento miRol) {

    /** Lanza 403 si mi rol no permite editar (subir documentos, crear tareas...). */
    public void exigirEdicion() {
        if (!miRol.puedeEditar()) {
            throw new AccesoDenegadoException("Tu rol en este evento (" + miRol + ") no permite modificarlo");
        }
    }

    /** Lanza 403 si no soy el propietario (invitar, cambiar roles...). */
    public void exigirAdministracion() {
        if (!miRol.puedeAdministrar()) {
            throw new AccesoDenegadoException("Solo el propietario puede hacer esto");
        }
    }
}
