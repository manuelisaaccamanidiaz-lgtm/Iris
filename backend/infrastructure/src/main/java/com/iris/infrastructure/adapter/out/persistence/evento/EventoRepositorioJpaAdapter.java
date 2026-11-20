package com.iris.infrastructure.adapter.out.persistence.evento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.iris.domain.evento.Evento;
import com.iris.domain.evento.EventoRepositorio;

/** Adaptador de salida: conecta el puerto EventoRepositorio con PostgreSQL vía JPA. */
@Component
class EventoRepositorioJpaAdapter implements EventoRepositorio {

    private final EventoJpaRepository jpa;

    EventoRepositorioJpaAdapter(EventoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Evento guardar(Evento evento) {
        return aDominio(jpa.save(aEntity(evento)));
    }

    @Override
    public List<Evento> listarPorUsuario(UUID usuarioId) {
        return jpa.findByUsuarioIdOrderByFechaInicioAsc(usuarioId).stream().map(this::aDominio).toList();
    }

    @Override
    public Optional<Evento> buscarPorIdYUsuario(UUID id, UUID usuarioId) {
        return jpa.findByIdAndUsuarioId(id, usuarioId).map(this::aDominio);
    }

    private EventoEntity aEntity(Evento e) {
        return new EventoEntity(e.id(), e.usuarioId(), e.nombre(), e.descripcion(), e.lugar(),
                e.fechaInicio(), e.fechaFin(), e.creadoEn());
    }

    private Evento aDominio(EventoEntity e) {
        return new Evento(e.getId(), e.getUsuarioId(), e.getNombre(), e.getDescripcion(), e.getLugar(),
                e.getFechaInicio(), e.getFechaFin(), e.getCreadoEn());
    }
}
