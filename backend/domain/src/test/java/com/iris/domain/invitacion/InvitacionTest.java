package com.iris.domain.invitacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.evento.RolEnEvento;
import com.iris.domain.usuario.Email;

class InvitacionTest {

    private Invitacion nueva() {
        return Invitacion.crear(UUID.randomUUID(), new Email("Carla@Mail.com"), RolEnEvento.EDITOR, UUID.randomUUID());
    }

    @Test
    void guardaElEmailNormalizadoYVenceEnSieteDias() {
        Invitacion inv = nueva();

        assertEquals("carla@mail.com", inv.email());
        assertTrue(inv.esPara(new Email("CARLA@mail.com")));
        assertEquals(Invitacion.VIGENCIA, java.time.Duration.between(inv.creadaEn(), inv.expiraEn()));
    }

    @Test
    void aceptarCierraLaInvitacion() {
        Invitacion inv = nueva();

        Invitacion aceptada = inv.aceptar(Instant.now());   // inmutable: guardamos la NUEVA

        assertEquals(EstadoInvitacion.ACEPTADA, aceptada.estado());
        assertNotNull(aceptada.cerradaEn());
    }

    @Test
    void noSePuedeResponderDosVeces() {
        Invitacion rechazada = nueva().rechazar(Instant.now());

        assertThrows(ConflictoException.class, () -> rechazada.aceptar(Instant.now()));
    }

    @Test
    void unaInvitacionVencidaNoSePuedeAceptar() {
        Invitacion inv = nueva();
        Instant dentroDeOchoDias = inv.creadaEn().plus(java.time.Duration.ofDays(8));

        assertTrue(inv.estaVencida(dentroDeOchoDias));
        assertThrows(ConflictoException.class, () -> inv.aceptar(dentroDeOchoDias));
    }

    @Test
    void vencerUsaLaFechaEnQueVencioDeVerdad() {
        Invitacion inv = nueva();
        Instant dentroDeOchoDias = inv.creadaEn().plus(java.time.Duration.ofDays(8));

        Invitacion vencida = inv.vencer(dentroDeOchoDias);

        assertEquals(EstadoInvitacion.VENCIDA, vencida.estado());
        assertEquals(inv.expiraEn(), vencida.cerradaEn());
    }
}
