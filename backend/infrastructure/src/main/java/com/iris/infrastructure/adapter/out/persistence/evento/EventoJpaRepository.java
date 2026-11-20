package com.iris.infrastructure.adapter.out.persistence.evento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data genera las consultas a partir del NOMBRE del método. */
interface EventoJpaRepository extends JpaRepository<EventoEntity, UUID> {

    // WHERE usuario_id = ? ORDER BY fecha_inicio ASC
    List<EventoEntity> findByUsuarioIdOrderByFechaInicioAsc(UUID usuarioId);

    // WHERE id = ? AND usuario_id = ?   <- el filtro multi-tenant
    Optional<EventoEntity> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
