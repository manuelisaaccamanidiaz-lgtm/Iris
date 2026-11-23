package com.iris.infrastructure.adapter.out.persistence.documento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.iris.domain.documento.EstadoDocumento;

interface DocumentoJpaRepository extends JpaRepository<DocumentoEntity, UUID> {

    // @Modifying: no es un SELECT; devuelve cuántas filas borró. El CASCADE de fragmentos lo hace la BD
    @Modifying
    @Query("delete from DocumentoEntity d where d.id = :id and d.estado <> :estado")
    int borrarSiNoEstaEn(@Param("id") UUID id, @Param("estado") EstadoDocumento estado);

    Optional<DocumentoEntity> findByEventoIdAndHashSha256(UUID eventoId, String hashSha256);

    List<DocumentoEntity> findByEventoIdOrderByCreadoEnDesc(UUID eventoId);

    // Solo traemos los ids (no la fila completa): es todo lo que necesita el re-encolado
    @Query("select d.id from DocumentoEntity d where d.estado in :estados order by d.creadoEn")
    List<UUID> idsConEstado(List<EstadoDocumento> estados);
}
