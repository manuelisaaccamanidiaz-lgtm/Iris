package com.iris.domain.invitacion;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.evento.RolEnEvento;
import com.iris.domain.usuario.Email;

/**
 * Una invitación a unirse a un evento, dirigida a un EMAIL (el invitado puede no tener cuenta aún).
 * Inmutable: aceptar, rechazar, cancelar o vencer devuelven una Invitacion nueva.
 *
 * @param email      siempre normalizado (minúsculas), igual que el CHECK de la V10
 * @param cerradaEn  cuándo dejó de estar PENDIENTE; null mientras siga pendiente
 */
public record Invitacion(
        UUID id,
        UUID eventoId,
        String email,
        RolEnEvento rol,
        EstadoInvitacion estado,
        UUID invitadoPor,
        Instant creadaEn,
        Instant expiraEn,
        Instant cerradaEn) {

    /** Decisión de Isaac (4B): una invitación vence a los 7 días. */
    public static final Duration VIGENCIA = Duration.ofDays(7);

    public Invitacion {
        if (eventoId == null || rol == null || estado == null || creadaEn == null || expiraEn == null) {
            throw new ReglaDeNegocioException("Faltan datos obligatorios de la invitación");
        }
        email = new Email(email).normalizado(); // valida el formato y lo pasa a minúsculas
        if (!expiraEn.isAfter(creadaEn)) {
            throw new ReglaDeNegocioException("La invitación debe vencer después de crearse");
        }
        // Misma regla que el CHECK ck_invitaciones_cerrada
        if ((estado == EstadoInvitacion.PENDIENTE) != (cerradaEn == null)) {
            throw new ReglaDeNegocioException("Una invitación pendiente no tiene fecha de cierre (y una cerrada sí)");
        }
    }

    public static Invitacion crear(UUID eventoId, Email email, RolEnEvento rol, UUID invitadoPor) {
        Instant ahora = Instant.now();
        return new Invitacion(null, eventoId, email.normalizado(), rol, EstadoInvitacion.PENDIENTE,
                invitadoPor, ahora, ahora.plus(VIGENCIA), null);
    }

    /** ¿Sigue marcada como PENDIENTE pero ya pasó su fecha? (todavía nadie la marcó VENCIDA) */
    public boolean estaVencida(Instant ahora) {
        return estado == EstadoInvitacion.PENDIENTE && !ahora.isBefore(expiraEn);
    }

    /** ¿Va dirigida a este email? (comparando normalizado: Ana@x.com y ana@x.com son la misma) */
    public boolean esPara(Email otro) {
        return email.equals(otro.normalizado());
    }

    public Invitacion aceptar(Instant ahora) {
        return cerrar(EstadoInvitacion.ACEPTADA, ahora);
    }

    public Invitacion rechazar(Instant ahora) {
        return cerrar(EstadoInvitacion.RECHAZADA, ahora);
    }

    public Invitacion cancelar(Instant ahora) {
        return cerrar(EstadoInvitacion.CANCELADA, ahora);
    }

    /** Marca como VENCIDA una invitación cuyo plazo ya pasó. La fecha de cierre es cuando venció de verdad. */
    public Invitacion vencer(Instant ahora) {
        if (!estaVencida(ahora)) {
            throw new ReglaDeNegocioException("Esta invitación no está vencida");
        }
        return new Invitacion(id, eventoId, email, rol, EstadoInvitacion.VENCIDA,
                invitadoPor, creadaEn, expiraEn, expiraEn);
    }

    /** Guarda común de aceptar/rechazar/cancelar: solo se cierra lo que está pendiente Y vigente. */
    private Invitacion cerrar(EstadoInvitacion nuevo, Instant ahora) {
        if (estado != EstadoInvitacion.PENDIENTE) {
            throw new ConflictoException("Esta invitación ya fue " + estado.name().toLowerCase());
        }
        if (estaVencida(ahora)) {
            throw new ConflictoException("Esta invitación venció; pide que te inviten de nuevo");
        }
        return new Invitacion(id, eventoId, email, rol, nuevo, invitadoPor, creadaEn, expiraEn, ahora);
    }
}
