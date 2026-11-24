package com.iris.application.documento;

import java.util.List;
import java.util.UUID;

import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.documento.Documento;
import com.iris.domain.documento.DocumentoRepositorio;
import com.iris.domain.evento.EventoRepositorio;

/** Caso de uso: ver los documentos de un evento y su estado. Cualquier miembro (incluso LECTOR). */
public class ListarDocumentos {

    private final EventoRepositorio eventos;
    private final DocumentoRepositorio documentos;

    public ListarDocumentos(EventoRepositorio eventos, DocumentoRepositorio documentos) {
        this.eventos = eventos;
        this.documentos = documentos;
    }

    public List<Documento> ejecutar(UUID usuarioId, UUID eventoId) {
        eventos.buscarComoMiembro(eventoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
        return documentos.listarPorEvento(eventoId);
    }
}
