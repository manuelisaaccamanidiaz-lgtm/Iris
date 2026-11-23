package com.iris.domain.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class TipoNotificacionTest {

    private final LocalDate hoy = LocalDate.of(2026, 12, 9);

    @Test
    void venceMananaVenceHoyYVencida() {
        assertEquals(Optional.of(TipoNotificacion.TAREA_VENCE_MANANA),
                TipoNotificacion.recordatorioPara(hoy.plusDays(1), hoy));
        assertEquals(Optional.of(TipoNotificacion.TAREA_VENCE_HOY),
                TipoNotificacion.recordatorioPara(hoy, hoy));
        assertEquals(Optional.of(TipoNotificacion.TAREA_VENCIDA),
                TipoNotificacion.recordatorioPara(hoy.minusDays(3), hoy));
    }

    @Test
    void sinRecordatorioSiFaltaMasDeUnDiaOSiNoTieneFecha() {
        assertTrue(TipoNotificacion.recordatorioPara(hoy.plusDays(2), hoy).isEmpty());
        assertTrue(TipoNotificacion.recordatorioPara(null, hoy).isEmpty());
    }

    @Test
    void soloLaInvitacionYLosRecordatoriosEnvianCorreo() {
        assertTrue(TipoNotificacion.INVITACION_RECIBIDA.enviaCorreo());
        assertTrue(TipoNotificacion.TAREA_VENCIDA.enviaCorreo());
        assertFalse(TipoNotificacion.TAREA_NUEVA.enviaCorreo());
    }
}
