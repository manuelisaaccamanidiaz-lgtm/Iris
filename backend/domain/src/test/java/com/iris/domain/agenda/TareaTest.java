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
        tarea.marcarHecha();

        assertNotNull(tarea.completadaEn());
        assertEquals(EstadoTarea.HECHA, tarea.estado());
    }

    @Test 
    void reabrirBorraLaFechaDeCompletado() {
        Tarea tarea = Tarea.crear(UUID.randomUUID(), new Tarea.Datos("Pagar", null, null, null), OrigenActividad.MANUAL, null);
        tarea.marcarHecha();

        assertNull(tarea.completadaEn());
    }
}
