package com.iris.infrastructure.adapter.out.persistence.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SesionJpaRepository extends JpaRepository<SesionEntity, UUID> {

    // WHERE evento_id = ? ORDER BY fecha ASC, hora_inicio ASC
    List<SesionEntity> findByEventoIdOrderByFechaAscHoraInicioAsc(UUID eventoId);

    // WHERE id = ? AND evento_id = ?  (una sesión de OTRO evento "no existe")
    Optional<SesionEntity> findByIdAndEventoId(UUID id, UUID eventoId);
}
