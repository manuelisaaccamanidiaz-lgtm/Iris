package com.iris.domain.documento;

/**
 * Solo el TEXTO de un fragmento, sin su vector de 1536 números: para leer el documento
 * (propuestas de Iris) no hace falta cargar los embeddings, que pesan mucho más.
 */
public record TextoFragmento(int orden, int paginaInicio, int paginaFin, String seccion, String contenido,
                             int tokens) {
}
