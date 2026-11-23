package com.iris.domain.propuesta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.compartido.ReglaDeNegocioException;

class PropuestaTest {

    private Propuesta tarea() {
        return Propuesta.crear(UUID.randomUUID(), UUID.randomUUID(), "Contrato.pdf", TipoPropuesta.TAREA,
                "Pagar anticipo", "30% del total", LocalDate.of(2026, 12, 1), null, null, null, null,
                "el anticipo del 30% se pagará a más tardar el 1 de diciembre", 2, 2);
    }

    @Test
    void unaSesionSinHorasNoEsValida() {
        assertThrows(ReglaDeNegocioException.class, () -> Propuesta.crear(UUID.randomUUID(), null, "Crono.pdf",
                TipoPropuesta.SESION, "Montaje", null, LocalDate.of(2026, 12, 9), null, null, null, null,
                "el montaje será el día 9", 1, 1));
    }

    @Test
    void losAjustesReemplazanSoloLoQueViene() {
        Propuesta ajustada = tarea().conAjustes(new Propuesta.Ajustes(null, null, LocalDate.of(2026, 11, 30),
                null, null, null));

        assertEquals("Pagar anticipo", ajustada.titulo());                 // no vino: se queda el de Iris
        assertEquals(LocalDate.of(2026, 11, 30), ajustada.fecha());        // vino: lo reemplaza
    }

    @Test
    void noSePuedeAceptarDosVeces() {
        Propuesta aceptada = tarea().aceptar(UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        assertEquals(EstadoPropuesta.ACEPTADA, aceptada.estado());
        assertThrows(ConflictoException.class,
                () -> aceptada.aceptar(UUID.randomUUID(), UUID.randomUUID(), Instant.now()));
    }

    @Test
    void lasHorasDeUnaSesionDebenTenerSentido() {
        assertThrows(ReglaDeNegocioException.class, () -> Propuesta.crear(UUID.randomUUID(), null, "Crono.pdf",
                TipoPropuesta.SESION, "Montaje", null, LocalDate.of(2026, 12, 9), LocalTime.of(10, 0),
                LocalTime.of(8, 0), null, null, "el montaje será el día 9", 1, 1));
    }
}
