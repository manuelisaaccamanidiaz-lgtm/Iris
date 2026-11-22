package com.iris.application.agenda;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.agenda.OrigenActividad;
import com.iris.domain.agenda.Sesion;
import com.iris.domain.agenda.SesionRepositorio;
import com.iris.domain.evento.EventoDeMiembro;

/** Caso de uso: un EDITOR o PROPIETARIO agrega una sesión a la agenda. */
public class CrearSesion {

    private final AccesoEvento acceso;
    private final SesionRepositorio sesiones;

    public CrearSesion(AccesoEvento acceso, SesionRepositorio sesiones) {
        this.acceso = acceso;
        this.sesiones = sesiones;
    }

    public Sesion ejecutar(UUID usuarioId, UUID eventoId, Sesion.Datos datos) {
        EventoDeMiembro evento = acceso.comoEditor(eventoId, usuarioId);
        return sesiones.guardar(Sesion.crear(evento.evento(), datos, OrigenActividad.MANUAL, usuarioId));
    }
}
