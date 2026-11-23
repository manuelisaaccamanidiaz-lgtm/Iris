package com.iris.infrastructure.adapter.out.persistence.agenda;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Despertador (4.3). JOIN con eventos para descartar los que ya terminaron (decisión de Isaac).
    // "t.fechaLimite <= :hasta" deja fuera las tareas sin fecha: en SQL, NULL <= algo no es verdadero
    @Query("""
            select t from TareaEntity t, EventoEntity e
            where e.id = t.eventoId and t.estado = :estado
              and t.fechaLimite <= :hasta and e.fechaFin >= :hoy
            """)
    List<TareaEntity> pendientesQueVencenHasta(@Param("estado") EstadoTarea estado,
                                               @Param("hasta") LocalDate hasta, @Param("hoy") LocalDate hoy);
}
