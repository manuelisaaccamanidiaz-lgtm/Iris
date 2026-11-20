package com.iris.infrastructure.adapter.out.persistence.documento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DocumentoJpaRepository extends JpaRepository<DocumentoEntity, UUID> {

    Optional<DocumentoEntity> findByEventoIdAndHashSha256(UUID eventoId, String hashSha256);

    List<DocumentoEntity> findByEventoIdOrderByCreadoEnDesc(UUID eventoId);
}
