package com.iris.infrastructure.adapter.in.web.admin;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.port.out.GeneradorEmbeddings;
import com.iris.application.port.out.RedactorRespuestas;

/**
 * Herramienta de diagnóstico SOLO para ADMIN (/api/admin/** lo protege SecurityConfig):
 * comprueba que la clave de Gemini funciona y permite "ver" cómo piensa el modelo.
 * Útil también para explicar embeddings en la sustentación.
 */
@RestController
@RequestMapping("/api/admin/diagnostico")
class DiagnosticoIaController {

    private final GeneradorEmbeddings embeddings;
    private final RedactorRespuestas redactor;

    DiagnosticoIaController(GeneradorEmbeddings embeddings, RedactorRespuestas redactor) {
        this.embeddings = embeddings;
        this.redactor = redactor;
    }

    /** GET /api/admin/diagnostico/chat?mensaje=...  -> ¿funciona la clave de Groq? (sin RAG) */
    @GetMapping("/chat")
    Map<String, Object> chat(@RequestParam String mensaje) {
        long inicio = System.currentTimeMillis();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("modelo", redactor.modelo());
        try {
            r.put("respuesta", redactor.redactar(
                    "Eres un asistente de pruebas. Responde en español en una sola oración.", mensaje));
        } catch (Exception e) {
            // Solo en este endpoint de ADMIN mostramos la causa real: es una herramienta de diagnóstico
            Throwable causa = e;
            while (causa.getCause() != null && causa.getCause() != causa) {
                causa = causa.getCause();
            }
            r.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            r.put("causaRaiz", causa.getClass().getSimpleName() + ": " + causa.getMessage());
        }
        r.put("milisegundos", System.currentTimeMillis() - inicio);
        return r;
    }

    /** GET /api/admin/diagnostico/similitud?a=...&b=...  -> qué tan parecidos son dos textos (0 a 1). */
    @GetMapping("/similitud")
    Map<String, Object> similitud(@RequestParam String a, @RequestParam String b) {
        List<float[]> v = embeddings.paraDocumentos(List.of(a, b));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("modelo", embeddings.modelo());
        r.put("dimensiones", v.get(0).length);
        r.put("primeros5DeA", Arrays.copyOf(v.get(0), 5));
        r.put("similitudCoseno", Math.round(coseno(v.get(0), v.get(1)) * 1000) / 1000.0);
        return r;
    }

    /** Similitud coseno = 1 - distancia coseno (lo que calcula el operador <=> de pgvector). */
    private static double coseno(float[] x, float[] y) {
        double punto = 0, nx = 0, ny = 0;
        for (int i = 0; i < x.length; i++) {
            punto += x[i] * y[i];
            nx += x[i] * x[i];
            ny += y[i] * y[i];
        }
        return punto / (Math.sqrt(nx) * Math.sqrt(ny));
    }
}
