package com.iris.application.propuesta;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.iris.application.port.out.GeneradorEmbeddings;
import com.iris.domain.propuesta.Propuesta;

/**
 * Duplicados por SIGNIFICADO, no por letras (decisión de Isaac: opción A, embeddings de Gemini).
 * "Pagar el anticipo" y "Realizar el pago del 30 %" no comparten casi palabras, pero sus vectores
 * quedan cerca: es la MISMA idea del RAG (Día 3), usada ahora para comparar acciones entre sí.
 *
 * <pre>
 * títulos (propuestas + lo que existe + lo descartado) -> UNA tanda de embeddings -> coseno -> ¿>= umbral?
 * </pre>
 *
 * Si Gemini falla, se devuelven las propuestas tal cual: el filtro por título exacto del validador
 * ya se aplicó antes, así que nunca quedamos sin respuesta (degradar con elegancia, no caerse).
 */
public class DetectorDuplicados {

    private static final System.Logger LOG = System.getLogger(DetectorDuplicados.class.getName());

    /**
     * Desde qué parecido dos títulos son "la misma acción". Es un punto de partida: se CALIBRA mirando
     * el log, donde también salen los "casi iguales" (desde {@link #CERCANO}) que NO se descartaron.
     */
    static final double UMBRAL = 0.85;
    static final double CERCANO = 0.75;
    private static final int MAX_COMPARABLES = 60; // el mismo tope que las listas del prompt
    static final String NOTA_DESCARTADA = "La descartaste antes, pero el documento fija un plazo para esto.";

    private final GeneradorEmbeddings embeddings;

    public DetectorDuplicados(GeneradorEmbeddings embeddings) {
        this.embeddings = embeddings;
    }

    /**
     * @param motivos el mismo mapa de motivos del validador: aquí se suman los descartes por parecido
     */
    public List<Propuesta> filtrar(List<Propuesta> candidatas, List<String> existentes, List<String> descartadas,
                                   Map<String, Integer> motivos) {
        if (candidatas.isEmpty()) {
            return candidatas;
        }
        List<String> ex = existentes.stream().limit(MAX_COMPARABLES).toList();
        List<String> de = descartadas.stream().limit(MAX_COMPARABLES).toList();
        List<String> textos = new ArrayList<>();
        candidatas.forEach(p -> textos.add(p.titulo()));
        textos.addAll(ex);
        textos.addAll(de);

        List<float[]> v;
        try {
            v = embeddings.paraDocumentos(textos); // UNA tanda para todo (el adaptador la parte en lotes)
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Sin filtro semántico de duplicados (Gemini falló): " + e.getMessage());
            return candidatas;
        }
        int n = candidatas.size();
        List<float[]> vCand = v.subList(0, n);
        List<float[]> vEx = v.subList(n, n + ex.size());
        List<float[]> vDe = v.subList(n + ex.size(), v.size());

        List<Propuesta> quedan = new ArrayList<>();
        List<float[]> vQuedan = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Propuesta p = candidatas.get(i);
            float[] yo = vCand.get(i);

            Parecida ya = masParecida(yo, vEx, ex, p.titulo());
            if (ya.parecido() >= UMBRAL) {
                motivos.merge("parecida a algo que ya existe", 1, Integer::sum);
                continue;
            }
            Parecida hermana = masParecida(yo, vQuedan, quedan.stream().map(Propuesta::titulo).toList(), p.titulo());
            if (hermana.parecido() >= UMBRAL) {
                motivos.merge("parecida a otra del mismo lote", 1, Integer::sum);
                continue;
            }
            Parecida descartada = masParecida(yo, vDe, de, p.titulo());
            if (descartada.parecido() >= UMBRAL) {
                if (p.fecha() == null) {
                    motivos.merge("parecida a una que descartaste", 1, Integer::sum);
                    continue;
                }
                p = conNota(p); // misma regla que el validador: con plazo, vuelve con la nota
            }
            quedan.add(p);
            vQuedan.add(yo);
        }
        return quedan;
    }

    /** El texto más parecido de la lista y cuánto (registra en el log los casos dudosos, para calibrar). */
    private static Parecida masParecida(float[] yo, List<float[]> vectores, List<String> textos, String titulo) {
        Parecida mejor = new Parecida(null, 0);
        for (int j = 0; j < vectores.size(); j++) {
            double c = coseno(yo, vectores.get(j));
            if (c > mejor.parecido()) {
                mejor = new Parecida(textos.get(j), c);
            }
        }
        if (mejor.parecido() >= CERCANO) {
            LOG.log(Level.INFO, String.format("«%s» ≈ «%s» (%.2f)%s", titulo, mejor.texto(), mejor.parecido(),
                    mejor.parecido() >= UMBRAL ? " -> duplicada" : " -> se queda"));
        }
        return mejor;
    }

    /** Coseno: 1 = misma dirección (misma idea), 0 = nada que ver. Lo mismo que hace pgvector con <=>. */
    static double coseno(float[] a, float[] b) {
        double punto = 0, na = 0, nb = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            punto += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0 : punto / Math.sqrt(na * nb);
    }

    private static Propuesta conNota(Propuesta p) {
        String d = p.descripcion() == null ? "" : p.descripcion();
        if (d.contains(NOTA_DESCARTADA)) {
            return p; // el validador ya se la puso (coincidencia exacta de título)
        }
        String nueva = d.isBlank() ? NOTA_DESCARTADA : d + "\n\n" + NOTA_DESCARTADA;
        return p.conAjustes(new Propuesta.Ajustes(null, nueva, null, null, null, null));
    }

    private record Parecida(String texto, double parecido) {
    }
}
