package com.iris.application.propuesta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.iris.application.port.out.AnalizadorDocumentos.PropuestaIA;
import com.iris.domain.documento.TextoFragmento;
import com.iris.domain.evento.Evento;
import com.iris.domain.propuesta.Propuesta;
import com.iris.domain.propuesta.TipoPropuesta;

class ValidadorPropuestasTest {

    // Evento con id (como si ya estuviera guardado): Evento.crear() lo dejaría en null
    private final Evento evento = new Evento(UUID.randomUUID(), UUID.randomUUID(), "Feria", null, null,
            LocalDate.of(2026, 12, 9), LocalDate.of(2026, 12, 11), Instant.now());

    private final LocalDate hoy = LocalDate.of(2026, 11, 24);

    private final List<TextoFragmento> fragmentos = List.of(new TextoFragmento(0, 2, 2, "PAGOS",
            "CLÁUSULA 4. El Cliente pagará un anticipo del 30% a más tardar el 1 de diciembre de 2026. "
                    + "CLÁUSULA 5. El montaje de stands se hará el 9 de diciembre.", 40));

    private final ValidadorPropuestas.Contexto vacio = ValidadorPropuestas.Contexto.de(hoy, List.of(), List.of());

    private PropuestaIA ia(String titulo, String fecha, String cita) {
        return new PropuestaIA("TAREA", titulo, "30%", fecha, "", "", "", "", 1, cita);
    }

    private List<Propuesta> validar(PropuestaIA p, ValidadorPropuestas.Contexto contexto) {
        return ValidadorPropuestas.validar(List.of(p), fragmentos, UUID.randomUUID(), "Contrato.pdf", evento, contexto)
                .validas();
    }

    @Test
    void unaCitaCopiadaConPequenosErroresSeQuedaConLaOracionReal() {
        // La IA cambió "pagará" por "pagara" y quitó el "%": la v1 la habría descartado
        List<Propuesta> r = validar(ia("Pagar anticipo", "2026-12-01", "el cliente pagara un anticipo del 30 a mas tardar"), vacio);

        assertEquals(1, r.size());
        assertTrue(r.get(0).cita().startsWith("El Cliente pagará un anticipo del 30%")); // texto REAL del documento
        assertEquals(2, r.get(0).paginaInicio()); // la página sale del fragmento, no de la IA
    }

    @Test
    void unaCitaInventadaSeDescarta() {
        assertTrue(validar(ia("Pedir desayuno", "", "el hotel regalará el desayuno a todos los invitados"), vacio).isEmpty());
    }

    @Test
    void loQueYaExisteNoSeVuelveAProponer() {
        var contexto = ValidadorPropuestas.Contexto.de(hoy, List.of("PAGAR ANTICIPO"), List.of());

        assertTrue(validar(ia("Pagar anticipo", "2026-12-01", "pagará un anticipo del 30% a más tardar"), contexto).isEmpty());
    }

    @Test
    void unaDescartadaSinPlazoNoVuelve() {
        var contexto = ValidadorPropuestas.Contexto.de(hoy, List.of(), List.of("Pagar anticipo"));

        assertTrue(validar(ia("Pagar anticipo", "", "pagará un anticipo del 30% a más tardar"), contexto).isEmpty());
    }

    @Test
    void unaDescartadaConPlazoVuelveConSuNota() {
        var contexto = ValidadorPropuestas.Contexto.de(hoy, List.of(), List.of("Pagar anticipo"));

        Propuesta p = validar(ia("Pagar anticipo", "2026-12-01", "pagará un anticipo del 30% a más tardar"), contexto).get(0);

        assertTrue(p.descripcion().contains("La descartaste antes"));
    }

    @Test
    void unPlazoQueYaPasoSeProponeParaHoyConNota() {
        Propuesta p = validar(ia("Pagar anticipo", "2026-03-15", "pagará un anticipo del 30% a más tardar"), vacio).get(0);

        assertEquals(hoy, p.fecha());
        assertTrue(p.descripcion().contains("ese plazo ya pasó"));
    }

    @Test
    void unaSesionFueraDelEventoSeConvierteEnTarea() {
        var sesion = new PropuestaIA("SESION", "Montaje", "", "2026-12-01", "08:00", "10:00", "", "LOGISTICA", 1,
                "el montaje de stands se hará el 9 de diciembre");

        Propuesta p = validar(sesion, vacio).get(0);

        assertEquals(TipoPropuesta.TAREA, p.tipo());
        assertNull(p.horaInicio());
    }

    @Test
    void laCitaVerificadaEncuentraLaOracionMasParecida() {
        assertNotNull(ValidadorPropuestas.citaVerificada("montaje de stands el 9 de diciembre", fragmentos.get(0).contenido()));
        assertNull(ValidadorPropuestas.citaVerificada("el pago", fragmentos.get(0).contenido())); // muy corta
    }
}
