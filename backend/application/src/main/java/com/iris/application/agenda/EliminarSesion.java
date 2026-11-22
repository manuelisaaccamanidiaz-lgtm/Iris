package com.iris.application.agenda;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.agenda.SesionRepositorio;
import com.iris.domain.compartido.RecursoNoEncontradoException;

/** Caso de uso: quitar una sesión de la agenda (EDITOR o PROPIETARIO). */
public class EliminarSesion {

    private final AccesoEvento acceso;
    private final SesionRepositorio sesiones;

    public EliminarSesion(AccesoEvento acceso, SesionRepositorio sesiones) {
        this.acceso = acceso;
        this.sesiones = sesiones;
    }

    public void ejecutar(UUID usuarioId, UUID eventoId, UUID sesionId) {
        acceso.comoEditor(eventoId, usuarioId);
        sesiones.buscarEnEvento(sesionId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));
        sesiones.eliminar(sesionId);
    }
}
