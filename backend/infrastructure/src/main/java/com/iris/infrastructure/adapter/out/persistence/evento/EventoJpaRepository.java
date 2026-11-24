package com.iris.infrastructure.adapter.out.persistence.evento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EventoJpaRepository extends JpaRepository<EventoEntity, UUID> {

    @Query("""
            select count(e) > 0 from EventoEntity e
            where e.creadoPor = :creadoPor and lower(e.nombre) = lower(:nombre) and e.fechaInicio = :fechaInicio
            """)
    boolean existeDelCreador(@Param("creadoPor") UUID creadoPor, @Param("nombre") String nombre,
                             @Param("fechaInicio") java.time.LocalDate fechaInicio);

    // JOIN con miembros: solo los eventos donde :usuarioId es miembro, junto con su rol.
    // V10: los que la persona ARCHIVÓ no aparecen en su lista (pero sigue pudiendo abrirlos por id)
    @Query("""
            select new com.iris.infrastructure.adapter.out.persistence.evento.EventoConRolFila(e, m.rol)
            from EventoEntity e, MiembroEventoEntity m
            where m.id.eventoId = e.id and m.id.usuarioId = :usuarioId and m.archivadoEn is null
            order by e.fechaInicio asc
            """)
    List<EventoConRolFila> listarDeMiembro(@Param("usuarioId") UUID usuarioId);

    @Query("""
            select new com.iris.infrastructure.adapter.out.persistence.evento.EventoConRolFila(e, m.rol)
            from EventoEntity e, MiembroEventoEntity m
            where m.id.eventoId = e.id and e.id = :eventoId and m.id.usuarioId = :usuarioId
            """)
    Optional<EventoConRolFila> buscarComoMiembro(@Param("eventoId") UUID eventoId,
                                                 @Param("usuarioId") UUID usuarioId);
}
