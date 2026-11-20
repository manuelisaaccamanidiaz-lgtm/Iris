package com.iris.infrastructure.adapter.out.persistence.evento;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.iris.domain.evento.Evento;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.EventoRepositorio;
import com.iris.domain.evento.RolEnEvento;

/** Adaptador de salida: eventos + membresías en PostgreSQL vía JPA. */
@Component
class EventoRepositorioJpaAdapter implements EventoRepositorio {

    private final EventoJpaRepository eventos;
    private final MiembroEventoJpaRepository miembros;

    EventoRepositorioJpaAdapter(EventoJpaRepository eventos, MiembroEventoJpaRepository miembros) {
        this.eventos = eventos;
        this.miembros = miembros;
    }

    /**
     * @Transactional: los dos INSERT (evento y propietario) van en UNA transacción.
     * Si el segundo falla, el primero se deshace (rollback): nunca queda un evento sin dueño.
     */
    @Override
    @Transactional
    public EventoDeMiembro crearConPropietario(Evento evento) {
        EventoEntity guardado = eventos.save(aEntity(evento));
        miembros.save(new MiembroEventoEntity(
                new MiembroEventoId(guardado.getId(), evento.creadoPor()),
                RolEnEvento.PROPIETARIO,
                Instant.now()));
        return new EventoDeMiembro(aDominio(guardado), RolEnEvento.PROPIETARIO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDeMiembro> listarDeMiembro(UUID usuarioId) {
        return eventos.listarDeMiembro(usuarioId).stream().map(this::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EventoDeMiembro> buscarComoMiembro(UUID eventoId, UUID usuarioId) {
        return eventos.buscarComoMiembro(eventoId, usuarioId).map(this::aDominio);
    }

    private EventoDeMiembro aDominio(EventoConRolFila fila) {
        return new EventoDeMiembro(aDominio(fila.evento()), fila.rol());
    }

    private EventoEntity aEntity(Evento e) {
        return new EventoEntity(e.id(), e.creadoPor(), e.nombre(), e.descripcion(), e.lugar(),
                e.fechaInicio(), e.fechaFin(), e.creadoEn());
    }

    private Evento aDominio(EventoEntity e) {
        return new Evento(e.getId(), e.getCreadoPor(), e.getNombre(), e.getDescripcion(), e.getLugar(),
                e.getFechaInicio(), e.getFechaFin(), e.getCreadoEn());
    }
}
