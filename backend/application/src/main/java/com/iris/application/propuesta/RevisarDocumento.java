package com.iris.application.propuesta;

import java.util.List;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.documento.Documento;
import com.iris.domain.documento.DocumentoRepositorio;
import com.iris.domain.documento.EstadoDocumento;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.propuesta.Propuesta;

/** Caso de uso, disparador BAJO PEDIDO: "Iris, revisa este documento" (EDITOR o PROPIETARIO). */
public class RevisarDocumento {

    private final AccesoEvento acceso;
    private final DocumentoRepositorio documentos;
    private final GenerarPropuestas generar;

    public RevisarDocumento(AccesoEvento acceso, DocumentoRepositorio documentos, GenerarPropuestas generar) {
        this.acceso = acceso;
        this.documentos = documentos;
        this.generar = generar;
    }

    public List<Propuesta> ejecutar(UUID yo, UUID eventoId, UUID documentoId) {
        EventoDeMiembro evento = acceso.comoEditor(eventoId, yo);
        Documento doc = documentos.buscarParaProcesar(documentoId)
                .filter(d -> d.eventoId().equals(eventoId)) // de otro evento = "no existe" aquí
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));
        if (doc.estado() != EstadoDocumento.INDEXADO) {
            throw new ConflictoException("El documento todavía no está indexado (estado: " + doc.estado() + ")");
        }
        return generar.ejecutar(doc, evento.evento());
    }
}
