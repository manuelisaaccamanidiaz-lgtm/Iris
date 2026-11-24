package com.iris.domain.documento;

import java.util.UUID;

import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Un pedazo de un documento, listo para buscar: su texto, de dónde salió (páginas, sección,
 * orden) y su embedding. Las páginas son la base de las citas "[1] Contrato.pdf · págs. 2–3".
 */
public record Fragmento(
        UUID documentoId,
        UUID eventoId,
        int orden,
        int paginaInicio,
        int paginaFin,
        String seccion,
        String contenido,
        int tokens,
        float[] embedding) {

    public Fragmento {
        if (documentoId == null || eventoId == null || contenido == null || contenido.isBlank()
                || embedding == null) {
            throw new ReglaDeNegocioException("Faltan datos obligatorios del fragmento");
        }
        if (paginaInicio < 1 || paginaFin < paginaInicio) {
            throw new ReglaDeNegocioException("Rango de páginas inválido");
        }
    }
}
