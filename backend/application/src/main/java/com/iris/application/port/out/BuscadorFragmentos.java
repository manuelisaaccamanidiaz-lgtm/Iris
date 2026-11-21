package com.iris.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida: búsqueda HÍBRIDA (vectorial + texto completo, fusionadas con RRF)
 * dentro de los fragmentos de UN evento. La implementación vive en SQL (infrastructure).
 */
public interface BuscadorFragmentos {

    /**
     * @param usuarioId       se vuelve a verificar la membresía DENTRO del SQL (defensa en profundidad)
     * @param vectorPregunta  embedding de la pregunta (mismo modelo y dimensiones que el índice)
     * @param limite          cuántos fragmentos devolver al final (top-k)
     */
    List<FragmentoEncontrado> buscar(UUID eventoId, UUID usuarioId, String pregunta,
                                     float[] vectorPregunta, int limite);

    /**
     * Un fragmento encontrado y CÓMO se encontró. rangoVector o rangoTexto son null
     * si no apareció en esa lista. similitud = 1 - distancia coseno (null si no salió por vector).
     */
    record FragmentoEncontrado(
            UUID fragmentoId,
            UUID documentoId,
            String nombreArchivo,
            int paginaInicio,
            int paginaFin,
            String seccion,
            String contenido,
            Double similitud,
            Integer rangoVector,
            Integer rangoTexto,
            double puntajeRrf) {

        /** "4" o "4–5": así se muestra en la cita. */
        public String paginas() {
            return paginaInicio == paginaFin ? String.valueOf(paginaInicio) : paginaInicio + "–" + paginaFin;
        }
    }
}
