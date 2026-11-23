package com.iris.infrastructure.adapter.in.web.invitacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.iris.domain.invitacion.Invitacion;
import com.iris.domain.invitacion.InvitacionRecibida;

/** JSON de una invitación (lo comparten los dos controladores del paquete). */
record RespuestaInvitacion(UUID id, UUID eventoId, String email, String rol, String estado,
                           Instant creadaEn, Instant expiraEn, Instant cerradaEn,
                           String nombreEvento, LocalDate fechaInicio, LocalDate fechaFin, String invitadoPor) {

    /** Vista del propietario: no necesita repetir los datos de su propio evento. */
    static RespuestaInvitacion de(Invitacion i) {
        return new RespuestaInvitacion(i.id(), i.eventoId(), i.email(), i.rol().name(), i.estado().name(),
                i.creadaEn(), i.expiraEn(), i.cerradaEn(), null, null, null, null);
    }

    /** Vista del invitado (bandeja): sí necesita saber de qué evento se trata y quién lo invitó. */
    static RespuestaInvitacion de(InvitacionRecibida r) {
        Invitacion i = r.invitacion();
        return new RespuestaInvitacion(i.id(), i.eventoId(), i.email(), i.rol().name(), i.estado().name(),
                i.creadaEn(), i.expiraEn(), i.cerradaEn(),
                r.nombreEvento(), r.fechaInicio(), r.fechaFin(), r.invitadoPorNombre());
    }
}
