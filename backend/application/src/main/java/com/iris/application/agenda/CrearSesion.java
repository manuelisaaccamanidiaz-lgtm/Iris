package com.iris.application.agenda;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.application.notificacion.Avisos;
import com.iris.domain.agenda.OrigenActividad;
import com.iris.domain.agenda.Sesion;
import com.iris.domain.agenda.SesionRepositorio;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.notificacion.TipoNotificacion;

/** Caso de uso: un EDITOR o PROPIETARIO agrega una sesión a la agenda. */
public class CrearSesion {

    private final AccesoEvento acceso;
    private final SesionRepositorio sesiones;
    private final Avisos avisos;

    public CrearSesion(AccesoEvento acceso, SesionRepositorio sesiones, Avisos avisos) {
        this.acceso = acceso;
        this.sesiones = sesiones;
        this.avisos = avisos;
    }

    public Sesion ejecutar(UUID usuarioId, UUID eventoId, Sesion.Datos datos) {
        EventoDeMiembro evento = acceso.comoEditor(eventoId, usuarioId);
        Sesion nueva = sesiones.guardar(Sesion.crear(evento.evento(), datos, OrigenActividad.MANUAL, usuarioId));
        // Los propietarios se enteran de todo lo que se crea en su evento (menos de lo que crearon ellos)
        avisos.aPropietarios(eventoId, TipoNotificacion.SESION_NUEVA,
                "Nueva sesión en «" + evento.evento().nombre() + "»: " + nueva.titulo() + " (" + nueva.fecha() + ")",
                nueva.id(), usuarioId);
        return nueva;
    }
}
