package com.iris.application.propuesta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.iris.application.port.out.GeneradorEmbeddings;
import com.iris.domain.propuesta.Propuesta;
import com.iris.domain.propuesta.TipoPropuesta;

/**
 * Sin llamar a Gemini: un GeneradorEmbeddings FALSO con vectores de 2 dimensiones que elegimos a mano.
 * Así probamos la LÓGICA (umbral, descartadas, lotes, respaldo) sin internet ni gastar cuota.
 * Es la gran ventaja del puerto: la aplicación no sabe si detrás hay Gemini o este doble de prueba.
 */
class DetectorDuplicadosTest {

    /** Mismo "tema" = misma dirección del vector (coseno ~1); temas distintos = perpendiculares (coseno 0). */
    private static final Map<String, float[]> VECTORES = Map.of(
            "Pagar el anticipo", new float[] {1, 0},
            "Realizar el pago del 30 %", new float[] {0.98f, 0.05f}, // otras palabras, MISMA idea
            "Montar los stands", new float[] {0, 1},
            "Armar los stands", new float[] {0.03f, 0.99f});

    private static GeneradorEmbeddings falso() {
        return new GeneradorEmbeddings() {
            public List<float[]> paraDocumentos(List<String> textos) {
                return textos.stream().map(VECTORES::get).toList();
            }
            public float[] paraConsulta(String pregunta) { throw new UnsupportedOperationException(); }
            public String modelo() { return "falso"; }
            public int dimensiones() { return 2; }
        };
    }

    private static Propuesta propuesta(String titulo, LocalDate fecha) {
        return Propuesta.crear(UUID.randomUUID(), UUID.randomUUID(), "Contrato.pdf", TipoPropuesta.TAREA, titulo,
                null, fecha, null, null, null, null, "El Cliente pagará un anticipo.", 1, 1);
    }

    @Test
    void loQueSignificaLoMismoQueAlgoExistenteSeDescarta() {
        Map<String, Integer> motivos = new HashMap<>();
        List<Propuesta> r = new DetectorDuplicados(falso()).filtrar(
                List.of(propuesta("Realizar el pago del 30 %", null), propuesta("Montar los stands", null)),
                List.of("Pagar el anticipo"), List.of(), motivos);

        assertEquals(1, r.size());
        assertEquals("Montar los stands", r.get(0).titulo());
        assertEquals(1, (int) motivos.get("parecida a algo que ya existe"));
    }

    @Test
    void dosPropuestasDelMismoLoteQueSignificanLoMismoQuedanEnUna() {
        List<Propuesta> r = new DetectorDuplicados(falso()).filtrar(
                List.of(propuesta("Montar los stands", null), propuesta("Armar los stands", null)),
                List.of(), List.of(), new HashMap<>());

        assertEquals(1, r.size());
    }

    @Test
    void unaParecidaADescartadaVuelveSoloSiTienePlazoYConNota() {
        DetectorDuplicados d = new DetectorDuplicados(falso());

        assertTrue(d.filtrar(List.of(propuesta("Realizar el pago del 30 %", null)),
                List.of(), List.of("Pagar el anticipo"), new HashMap<>()).isEmpty());

        List<Propuesta> conPlazo = d.filtrar(List.of(propuesta("Realizar el pago del 30 %", LocalDate.of(2026, 12, 1))),
                List.of(), List.of("Pagar el anticipo"), new HashMap<>());
        assertEquals(1, conPlazo.size());
        assertTrue(conPlazo.get(0).descripcion().contains(DetectorDuplicados.NOTA_DESCARTADA));
    }

    @Test
    void siGeminiFallaSeDevuelvenLasPropuestasTalCual() {
        GeneradorEmbeddings caido = new GeneradorEmbeddings() {
            public List<float[]> paraDocumentos(List<String> textos) { throw new IllegalStateException("429"); }
            public float[] paraConsulta(String pregunta) { throw new UnsupportedOperationException(); }
            public String modelo() { return "caido"; }
            public int dimensiones() { return 2; }
        };
        List<Propuesta> r = new DetectorDuplicados(caido).filtrar(
                List.of(propuesta("Montar los stands", null)), List.of("Armar los stands"), List.of(), new HashMap<>());

        assertEquals(1, r.size()); // degradar con elegancia: sin filtro semántico, pero CON respuesta
    }
}
