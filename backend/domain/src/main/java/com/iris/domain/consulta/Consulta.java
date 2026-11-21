package com.iris.domain.consulta;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Una pregunta a Iris con su respuesta, sus fuentes y datos de trazabilidad. */
public record Consulta(
        UUID id,
        UUID eventoId,
        UUID usuarioId,
        String pregunta,
        String respuesta,
        boolean sinEvidencia,
        List<FuenteCitada> fuentes,
        String modelo,
        int milisegundos,
        Instant creadoEn) {

    public Consulta {
        fuentes = List.copyOf(fuentes); // copia inmutable: nadie la modifica desde afuera
    }

    /** Solo las fuentes que el modelo citó de verdad (las que se muestran bajo la respuesta). */
    public List<FuenteCitada> fuentesCitadas() {
        return fuentes.stream().filter(FuenteCitada::citada).toList();
    }
}
