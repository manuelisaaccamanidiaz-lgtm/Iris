package com.iris.infrastructure.adapter.out.persistence.documento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.iris.domain.documento.EstadoDocumento;

interface DocumentoJpaRepository extends JpaRepository<DocumentoEntity, UUID> {

    Optional<DocumentoEntity> findByEventoIdAndHashSha256(UUID eventoId, String hashSha256);

    List<DocumentoEntity> findByEventoIdOrderByCreadoEnDesc(UUID eventoId);

    // Solo traemos los ids (no la fila completa): es todo lo que necesita el re-encolado
    @Query("select d.id from DocumentoEntity d where d.estado in :estados order by d.creadoEn")
    List<UUID> idsConEstado(List<EstadoDocumento> estados);
}
