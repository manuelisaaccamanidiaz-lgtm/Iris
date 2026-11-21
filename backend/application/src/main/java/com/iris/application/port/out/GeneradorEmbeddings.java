package com.iris.application.port.out;

import java.util.List;

/**
 * Puerto de salida: convierte texto en vectores. Hoy lo implementa Gemini vía Spring AI.
 * REGLA DE ORO del RAG: documentos y preguntas deben usar el MISMO modelo y las MISMAS
 * dimensiones; si no, los vectores no son comparables (como medir en metros y en pulgadas).
 */
public interface GeneradorEmbeddings {

    /** Vectores para fragmentos de documentos (se guardan en la BD). */
    List<float[]> paraDocumentos(List<String> textos);

    /** Vector para una pregunta del usuario (se usará en el Día 3). */
    float[] paraConsulta(String pregunta);

    /** Nombre del modelo, para la trazabilidad (columna modelo_embedding). */
    String modelo();

    int dimensiones();
}
