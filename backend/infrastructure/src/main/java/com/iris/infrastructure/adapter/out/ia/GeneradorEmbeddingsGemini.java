package com.iris.infrastructure.adapter.out.ia;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.GeneradorEmbeddings;
import com.iris.infrastructure.config.EmbeddingsPropiedades;

/** Adaptador: implementa el puerto GeneradorEmbeddings con los dos modelos de Gemini. */
@Component
class GeneradorEmbeddingsGemini implements GeneradorEmbeddings {

    // Enviamos los textos en lotes pequeños: menos riesgo de chocar con los límites del plan gratis
    private static final int TAMANO_LOTE = 20;

    private final EmbeddingModel documentos;
    private final EmbeddingModel consultas;
    private final EmbeddingsPropiedades propiedades;

    // @Qualifier: hay DOS beans EmbeddingModel; así le decimos a Spring cuál va en cada parámetro
    GeneradorEmbeddingsGemini(@Qualifier("embeddingsDocumentos") EmbeddingModel documentos,
                              @Qualifier("embeddingsConsultas") EmbeddingModel consultas,
                              EmbeddingsPropiedades propiedades) {
        this.documentos = documentos;
        this.consultas = consultas;
        this.propiedades = propiedades;
    }

    @Override
    public List<float[]> paraDocumentos(List<String> textos) {
        List<float[]> vectores = new ArrayList<>(textos.size());
        for (int i = 0; i < textos.size(); i += TAMANO_LOTE) {
            List<String> lote = textos.subList(i, Math.min(i + TAMANO_LOTE, textos.size()));
            documentos.embed(lote).forEach(v -> vectores.add(validar(v)));
        }
        return vectores;
    }

    @Override
    public float[] paraConsulta(String pregunta) {
        return validar(consultas.embed(pregunta));
    }

    @Override
    public String modelo() {
        return propiedades.modelo();
    }

    @Override
    public int dimensiones() {
        return propiedades.dimensiones();
    }

    /** Si Gemini devolviera otro tamaño, la BD (vector(1536)) lo rechazaría: mejor fallar con un mensaje claro. */
    private float[] validar(float[] vector) {
        if (vector.length != propiedades.dimensiones()) {
            throw new IllegalStateException("Gemini devolvió " + vector.length
                    + " dimensiones y se esperaban " + propiedades.dimensiones());
        }
        return vector;
    }
}
