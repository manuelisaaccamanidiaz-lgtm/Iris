package com.iris.application.agenda;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.agenda.Sesion;
import com.iris.domain.agenda.SesionRepositorio;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.evento.EventoDeMiembro;

/** Caso de uso: cambiar los datos de una sesión (EDITOR o PROPIETARIO). */
public class EditarSesion {

    private final AccesoEvento acceso;
    private final SesionRepositorio sesiones;

    public EditarSesion(AccesoEvento acceso, SesionRepositorio sesiones) {
        this.acceso = acceso;
        this.sesiones = sesiones;
    }

    public Sesion ejecutar(UUID usuarioId, UUID eventoId, UUID sesionId, Sesion.Datos datos) {
        EventoDeMiembro evento = acceso.comoEditor(eventoId, usuarioId);
        Sesion actual = sesiones.buscarEnEvento(sesionId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));
        return sesiones.guardar(actual.editar(evento.evento(), datos));
    }
}
