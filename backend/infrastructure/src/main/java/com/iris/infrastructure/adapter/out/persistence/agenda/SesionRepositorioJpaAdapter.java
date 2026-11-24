package com.iris.infrastructure.adapter.out.persistence.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.iris.domain.agenda.Sesion;
import com.iris.domain.agenda.SesionRepositorio;

@Component
class SesionRepositorioJpaAdapter implements SesionRepositorio {

    private final SesionJpaRepository jpa;

    SesionRepositorioJpaAdapter(SesionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Sesion guardar(Sesion sesion) {
        return aDominio(jpa.save(aEntity(sesion)));
    }

    @Override
    public List<Sesion> listarPorEvento(UUID eventoId) {
        return jpa.findByEventoIdOrderByFechaAscHoraInicioAsc(eventoId).stream().map(this::aDominio).toList();
    }

    @Override
    public Optional<Sesion> buscarEnEvento(UUID sesionId, UUID eventoId) {
        return jpa.findByIdAndEventoId(sesionId, eventoId).map(this::aDominio);
    }

    @Override
    public void eliminar(UUID sesionId) {
        jpa.deleteById(sesionId);
    }

    private SesionEntity aEntity(Sesion s) {
        return new SesionEntity(s.id(), s.eventoId(), s.titulo(), s.descripcion(), s.fecha(), s.horaInicio(),
                s.horaFin(), s.sala(), s.ponente(), s.tipo(), s.origen(), s.creadoPor(), s.creadoEn(),
                s.actualizadoEn());
    }

    private Sesion aDominio(SesionEntity e) {
        return new Sesion(e.getId(), e.getEventoId(), e.getTitulo(), e.getDescripcion(), e.getFecha(),
                e.getHoraInicio(), e.getHoraFin(), e.getSala(), e.getPonente(), e.getTipo(), e.getOrigen(),
                e.getCreadoPor(), e.getCreadoEn(), e.getActualizadoEn());
    }
}
