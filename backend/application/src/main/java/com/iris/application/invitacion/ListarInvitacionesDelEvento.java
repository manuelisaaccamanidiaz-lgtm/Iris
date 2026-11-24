package com.iris.application.invitacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.invitacion.Invitacion;
import com.iris.domain.invitacion.InvitacionRepositorio;

/** Caso de uso: un propietario ve todas las invitaciones de su evento (cualquier estado). */
public class ListarInvitacionesDelEvento {

    private final AccesoEvento acceso;
    private final InvitacionRepositorio invitaciones;

    public ListarInvitacionesDelEvento(AccesoEvento acceso, InvitacionRepositorio invitaciones) {
        this.acceso = acceso;
        this.invitaciones = invitaciones;
    }

    public List<Invitacion> ejecutar(UUID yo, UUID eventoId) {
        acceso.comoMiembro(eventoId, yo).exigirAdministracion();
        Instant ahora = Instant.now();
        // Las que ya vencieron pero nadie marcó se MUESTRAN como VENCIDA (sin escribir en la BD:
        // listar no debe tener efectos secundarios; el proceso diario del 4.3 las marca de verdad)
        return invitaciones.listarPorEvento(eventoId).stream()
                .map(inv -> inv.estaVencida(ahora) ? inv.vencer(ahora) : inv)
                .toList();
    }
}
