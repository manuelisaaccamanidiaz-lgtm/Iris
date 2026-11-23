package com.iris.application.invitacion;

import java.time.Instant;
import java.util.UUID;

import com.iris.domain.invitacion.InvitacionRepositorio;

/** Caso de uso: rechazo una invitación dirigida a mi email. Queda RECHAZADA (y me pueden reinvitar). */
public class RechazarInvitacion {

    private final InvitacionesDelUsuario delUsuario;
    private final InvitacionRepositorio invitaciones;

    public RechazarInvitacion(InvitacionesDelUsuario delUsuario, InvitacionRepositorio invitaciones) {
        this.delUsuario = delUsuario;
        this.invitaciones = invitaciones;
    }

    public void ejecutar(UUID yo, UUID invitacionId) {
        var invitacion = delUsuario.buscarMia(yo, invitacionId);
        invitaciones.guardar(invitacion.rechazar(Instant.now())); // 409 si ya no estaba pendiente o venció
    }
}
