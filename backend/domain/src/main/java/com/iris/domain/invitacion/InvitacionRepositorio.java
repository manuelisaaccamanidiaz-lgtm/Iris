package com.iris.domain.invitacion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida para invitaciones. Los emails que recibe ya vienen normalizados. */
public interface InvitacionRepositorio {

    /** Inserta si id es null; si no, actualiza (estado y fecha de cierre). */
    Invitacion guardar(Invitacion invitacion);

    Optional<Invitacion> buscarPorId(UUID id);

    /** La invitación PENDIENTE (aunque ya esté vencida en el tiempo) para ese email en ese evento. */
    Optional<Invitacion> buscarPendiente(UUID eventoId, String email);

    /** Todas las del evento (cualquier estado), de la más reciente a la más antigua. */
    List<Invitacion> listarPorEvento(UUID eventoId);

    /** Las PENDIENTES dirigidas a ese email, con los datos del evento, para la bandeja. */
    List<InvitacionRecibida> listarPendientesPara(String email);

    /** Despertador (4.3): PENDIENTE con expira_en <= ahora pasan a VENCIDA. Devuelve cuántas. */
    int vencerExpiradas(Instant ahora);
}
