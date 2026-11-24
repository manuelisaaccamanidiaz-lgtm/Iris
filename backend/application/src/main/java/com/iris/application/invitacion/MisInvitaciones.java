package com.iris.application.invitacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.iris.domain.invitacion.InvitacionRecibida;
import com.iris.domain.invitacion.InvitacionRepositorio;

/** Caso de uso: mi bandeja. Las invitaciones PENDIENTES y vigentes dirigidas a mi email. */
public class MisInvitaciones {

    private final InvitacionesDelUsuario delUsuario;
    private final InvitacionRepositorio invitaciones;

    public MisInvitaciones(InvitacionesDelUsuario delUsuario, InvitacionRepositorio invitaciones) {
        this.delUsuario = delUsuario;
        this.invitaciones = invitaciones;
    }

    public List<InvitacionRecibida> ejecutar(UUID yo) {
        Instant ahora = Instant.now();
        String miEmail = delUsuario.miEmail(yo).normalizado();
        // Funciona aunque me haya registrado DESPUÉS de que me invitaran: se busca por email
        return invitaciones.listarPendientesPara(miEmail).stream()
                .filter(r -> !r.invitacion().estaVencida(ahora)) // una vencida ya no se puede aceptar
                .toList();
    }
}
