package com.iris.application.consulta;

import java.util.List;
import java.util.UUID;

import com.iris.application.port.out.BuscadorFragmentos;
import com.iris.application.port.out.BuscadorFragmentos.FragmentoEncontrado;
import com.iris.application.port.out.GeneradorEmbeddings;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.evento.EventoRepositorio;

/**
 * Caso de uso: pasos 1 a 3 del flujo de consulta del profe (sin redactar respuesta todavía).
 * Lo usa el endpoint de búsqueda (para VER qué encuentra Iris) y lo usará la consulta con Groq.
 */
public class BuscarFragmentos {

    public static final int LIMITE = 5; // top-k: los 5 mejores fragmentos van al modelo

    private final EventoRepositorio eventos;
    private final GeneradorEmbeddings embeddings;
    private final BuscadorFragmentos buscador;

    public BuscarFragmentos(EventoRepositorio eventos, GeneradorEmbeddings embeddings,
                            BuscadorFragmentos buscador) {
        this.eventos = eventos;
        this.embeddings = embeddings;
        this.buscador = buscador;
    }

    public List<FragmentoEncontrado> ejecutar(UUID usuarioId, UUID eventoId, String pregunta) {
        // 0. ¿Es miembro? Cualquier rol puede preguntar (incluso LECTOR). Si no: 404
        eventos.buscarComoMiembro(eventoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));

        String limpia = pregunta == null ? "" : pregunta.strip();
        if (limpia.length() < 3) {
            throw new ReglaDeNegocioException("Escribe una pregunta un poco más completa");
        }
        if (limpia.length() > 1000) {
            throw new ReglaDeNegocioException("La pregunta es demasiado larga (máximo 1000 caracteres)");
        }

        // 1. Embedding de la pregunta con el MISMO modelo del índice (taskType RETRIEVAL_QUERY)
        float[] vector = embeddings.paraConsulta(limpia);

        // 2 y 3. Búsqueda vectorial + texto completo, fusionadas y reordenadas (RRF)
        return buscador.buscar(eventoId, usuarioId, limpia, vector, LIMITE);
    }
}
