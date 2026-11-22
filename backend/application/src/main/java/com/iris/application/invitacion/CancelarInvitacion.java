package com.iris.application.invitacion;

import java.time.Instant;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.invitacion.InvitacionRepositorio;

/** Caso de uso: un propietario cancela una invitación pendiente. No se borra: queda CANCELADA. */
public class CancelarInvitacion {

    private final AccesoEvento acceso;
    private final InvitacionRepositorio invitaciones;

    public CancelarInvitacion(AccesoEvento acceso, InvitacionRepositorio invitaciones) {
        this.acceso = acceso;
        this.invitaciones = invitaciones;
    }

    public void ejecutar(UUID yo, UUID eventoId, UUID invitacionId) {
        acceso.comoMiembro(eventoId, yo).exigirAdministracion();
        var invitacion = invitaciones.buscarPorId(invitacionId)
                .filter(inv -> inv.eventoId().equals(eventoId)) // de OTRO evento = "no existe" aquí
                .orElseThrow(() -> new RecursoNoEncontradoException("Invitación no encontrada"));
        invitaciones.guardar(invitacion.cancelar(Instant.now())); // 409 si ya no estaba pendiente
    }
}
