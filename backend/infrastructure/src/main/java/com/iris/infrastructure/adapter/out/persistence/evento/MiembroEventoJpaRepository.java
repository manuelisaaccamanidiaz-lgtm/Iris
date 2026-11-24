package com.iris.infrastructure.adapter.out.persistence.evento;

import org.springframework.data.jpa.repository.JpaRepository;

interface MiembroEventoJpaRepository extends JpaRepository<MiembroEventoEntity, MiembroEventoId> {
}
