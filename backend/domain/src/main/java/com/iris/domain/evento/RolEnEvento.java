package com.iris.domain.evento;

/**
 * Rol de una persona DENTRO de un evento (distinto del Rol global ORGANIZADOR/ADMIN).
 * Cada rol "sabe" qué puede hacer: las reglas de permisos viven en el dominio, en un solo lugar.
 */
public enum RolEnEvento {

    /** Todo: invitar, cambiar roles, quitar miembros, borrar el evento. */
    PROPIETARIO,
    /** Subir documentos, crear sesiones y tareas, aceptar propuestas de Iris. */
    EDITOR,
    /** Consultar a Iris y ver todo, sin modificar. */
    LECTOR;

    public boolean puedeEditar() {
        return this == PROPIETARIO || this == EDITOR;
    }

    public boolean puedeAdministrar() {
        return this == PROPIETARIO;
    }
}
