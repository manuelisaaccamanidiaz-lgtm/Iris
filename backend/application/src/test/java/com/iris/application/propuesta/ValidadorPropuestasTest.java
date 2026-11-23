package com.iris.application.propuesta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.iris.application.port.out.AnalizadorDocumentos.PropuestaIA;
import com.iris.domain.documento.TextoFragmento;
import com.iris.domain.evento.Evento;
import com.iris.domain.propuesta.TipoPropuesta;

class ValidadorPropuestasTest {

    // Evento con id (como si ya estuviera guardado): Evento.crear() lo dejaría en null
    private final Evento evento = new Evento(UUID.randomUUID(), UUID.randomUUID(), "Feria", null, null,
            LocalDate.of(2026, 12, 9), LocalDate.of(2026, 12, 11), Instant.now());

    private final List<TextoFragmento> fragmentos = List.of(new TextoFragmento(0, 2, 2, "PAGOS",
            "CLÁUSULA 4. El Cliente pagará un anticipo del 30% a más tardar el 1 de diciembre de 2026.", 25));

    private PropuestaIA ia(String tipo, String fecha, String inicio, String fin, int fragmento, String cita) {
        return new PropuestaIA(tipo, "Pagar anticipo", "30%", fecha, inicio, fin, "", "", fragmento, cita);
    }

    @Test
    void unaPropuestaConCitaTextualPasa() {
        var r = ValidadorPropuestas.validar(
                List.of(ia("TAREA", "2026-12-01", "", "", 1, "pagará un anticipo del 30% a más tardar")),
                fragmentos, UUID.randomUUID(), "Contrato.pdf", evento);

        assertEquals(1, r.validas().size());
        assertEquals(2, r.validas().get(0).paginaInicio()); // la página sale del fragmento, no de la IA
    }

    @Test
    void unaCitaInventadaSeDescarta() {
        var r = ValidadorPropuestas.validar(
                List.of(ia("TAREA", "", "", "", 1, "el hotel regalará el desayuno a todos")),
                fragmentos, UUID.randomUUID(), "Contrato.pdf", evento);

        assertEquals(0, r.validas().size());
        assertEquals(1, r.descartadas());
    }

    @Test
    void unFragmentoQueNoExisteSeDescarta() {
        var r = ValidadorPropuestas.validar(
                List.of(ia("TAREA", "", "", "", 7, "pagará un anticipo del 30% a más tardar")),
                fragmentos, UUID.randomUUID(), "Contrato.pdf", evento);

        assertEquals(0, r.validas().size());
    }

    @Test
    void unaSesionFueraDelEventoSeConvierteEnTarea() {
        var r = ValidadorPropuestas.validar(
                List.of(ia("SESION", "2026-12-01", "08:00", "10:00", 1, "pagará un anticipo del 30% a más tardar")),
                fragmentos, UUID.randomUUID(), "Contrato.pdf", evento);

        assertEquals(TipoPropuesta.TAREA, r.validas().get(0).tipo());
        assertEquals(null, r.validas().get(0).horaInicio());
    }

    @Test
    void laCitaSeComparaSinTildesNiMayusculas() {
        assertTrue(ValidadorPropuestas.citaEsTextual("PAGARA UN ANTICIPO del 30 %", fragmentos.get(0).contenido()));
        assertFalse(ValidadorPropuestas.citaEsTextual("el pago", fragmentos.get(0).contenido())); // muy corta
    }
}
