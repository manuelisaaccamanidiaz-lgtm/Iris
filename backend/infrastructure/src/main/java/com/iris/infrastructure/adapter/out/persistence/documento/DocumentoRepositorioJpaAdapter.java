package com.iris.infrastructure.adapter.out.persistence.documento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.iris.domain.documento.Documento;
import com.iris.domain.documento.DocumentoRepositorio;
import com.iris.domain.documento.EstadoDocumento;

@Component
class DocumentoRepositorioJpaAdapter implements DocumentoRepositorio {

    private final DocumentoJpaRepository jpa;

    DocumentoRepositorioJpaAdapter(DocumentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Documento guardar(Documento documento) {
        return aDominio(jpa.save(aEntity(documento)));
    }

    @Override
    public Optional<Documento> buscarParaProcesar(UUID id) {
        return jpa.findById(id).map(this::aDominio);
    }

    @Override
    public List<UUID> idsPendientes() {
        return jpa.idsConEstado(List.of(EstadoDocumento.EN_COLA, EstadoDocumento.PROCESANDO));
    }

    @Override
    public void eliminar(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public Optional<Documento> buscarPorEventoYHash(UUID eventoId, String hashSha256) {
        return jpa.findByEventoIdAndHashSha256(eventoId, hashSha256).map(this::aDominio);
    }

    @Override
    public List<Documento> listarPorEvento(UUID eventoId) {
        return jpa.findByEventoIdOrderByCreadoEnDesc(eventoId).stream().map(this::aDominio).toList();
    }

    private DocumentoEntity aEntity(Documento d) {
        return new DocumentoEntity(d.id(), d.eventoId(), d.subidoPor(), d.nombreArchivo(), d.tamanoBytes(),
                d.hashSha256(), d.paginas(), d.estado(), d.errorMensaje(), d.modeloEmbedding(),
                d.dimensiones(), d.rutaTemporal(), d.creadoEn(), d.actualizadoEn(), d.indexadoEn());
    }

    private Documento aDominio(DocumentoEntity e) {
        return new Documento(e.getId(), e.getEventoId(), e.getSubidoPor(), e.getNombreArchivo(),
                e.getTamanoBytes(), e.getHashSha256(), e.getPaginas(), e.getEstado(), e.getErrorMensaje(),
                e.getModeloEmbedding(), e.getDimensiones(), e.getRutaTemporal(), e.getCreadoEn(),
                e.getActualizadoEn(), e.getIndexadoEn());
    }
}
