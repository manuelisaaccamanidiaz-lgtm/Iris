package com.iris.application.notificacion;

import java.time.Instant;

import com.iris.domain.invitacion.InvitacionRepositorio;

/**
 * Caso de uso del DESPERTADOR: marca como VENCIDA cada invitación PENDIENTE cuyo plazo pasó
 * y que nadie tocó (la promesa del 4.2: "el proceso diario marca las demás").
 */
public class VencerInvitaciones {

    private final InvitacionRepositorio invitaciones;

    public VencerInvitaciones(InvitacionRepositorio invitaciones) {
        this.invitaciones = invitaciones;
    }

    /** @return cuántas se marcaron */
    public int ejecutar(Instant ahora) {
        return invitaciones.vencerExpiradas(ahora);
    }
}
