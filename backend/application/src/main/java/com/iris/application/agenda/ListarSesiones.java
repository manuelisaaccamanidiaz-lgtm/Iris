package com.iris.application.agenda;

import java.util.List;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.agenda.Sesion;
import com.iris.domain.agenda.SesionRepositorio;

/** Caso de uso: ver la agenda del evento. Cualquier miembro (incluso LECTOR). */
public class ListarSesiones {

    private final AccesoEvento acceso;
    private final SesionRepositorio sesiones;

    public ListarSesiones(AccesoEvento acceso, SesionRepositorio sesiones) {
        this.acceso = acceso;
        this.sesiones = sesiones;
    }

    public List<Sesion> ejecutar(UUID usuarioId, UUID eventoId) {
        acceso.comoMiembro(eventoId, usuarioId);
        return sesiones.listarPorEvento(eventoId);
    }
}
