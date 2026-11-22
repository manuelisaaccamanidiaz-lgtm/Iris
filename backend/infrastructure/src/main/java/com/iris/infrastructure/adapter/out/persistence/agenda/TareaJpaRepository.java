package com.iris.infrastructure.adapter.out.persistence.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iris.domain.agenda.EstadoTarea;

/**
 * Un método por combinación de filtros. Más largo que una sola consulta con "(:x IS NULL OR ...)",
 * pero evita el problema de PostgreSQL con parámetros nulos sin tipo, y cada consulta usa bien el índice.
 */
interface TareaJpaRepository extends JpaRepository<TareaEntity, UUID> {

    List<TareaEntity> findByEventoId(UUID eventoId);

    List<TareaEntity> findByEventoIdAndEstado(UUID eventoId, EstadoTarea estado);

    List<TareaEntity> findByEventoIdAndResponsableId(UUID eventoId, UUID responsableId);

    List<TareaEntity> findByEventoIdAndEstadoAndResponsableId(UUID eventoId, EstadoTarea estado, UUID responsableId);

    Optional<TareaEntity> findByIdAndEventoId(UUID id, UUID eventoId);
}
