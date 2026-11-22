package com.iris.domain.agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TareaTest {
    @Test
    void marcarHechaPoneFechaDeCompletado() {
        Tarea tarea = Tarea.crear(UUID.randomUUID(), new Tarea.Datos("Pagar", null, null, null), OrigenActividad.MANUAL, null);

        Tarea hecha = tarea.marcarHecha();          // guardamos la NUEVA

        assertEquals(EstadoTarea.HECHA, hecha.estado());
        assertNotNull(hecha.completadaEn());
    }

    @Test
    void reabrirBorraLaFechaDeCompletado() {
        Tarea tarea = Tarea.crear(UUID.randomUUID(), new Tarea.Datos("Pagar", null, null, null), OrigenActividad.MANUAL, null);

        Tarea reabierta = tarea.marcarHecha().reabrir();   // encadenado: cada paso devuelve una nueva

        assertEquals(EstadoTarea.PENDIENTE, reabierta.estado());
        assertNull(reabierta.completadaEn());
    }
}
