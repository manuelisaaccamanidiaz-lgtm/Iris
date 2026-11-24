package com.iris.infrastructure.adapter.out.persistence.agenda;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.iris.domain.agenda.EstadoTarea;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;

@Component
class TareaRepositorioJpaAdapter implements TareaRepositorio {

    /**
     * Orden de la lista: pendientes primero, luego por fecha límite (las que no tienen fecha al final),
     * y si empatan, la más antigua primero. Comparator encadena criterios con thenComparing.
     */
    private static final Comparator<Tarea> ORDEN = Comparator
            .comparing((Tarea t) -> t.estado() == EstadoTarea.HECHA)                 // false (pendiente) va antes
            .thenComparing(Tarea::fechaLimite, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
            .thenComparing(Tarea::creadoEn);

    private final TareaJpaRepository jpa;

    TareaRepositorioJpaAdapter(TareaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Tarea guardar(Tarea tarea) {
        return aDominio(jpa.save(aEntity(tarea)));
    }

    @Override
    public List<Tarea> listarPorEvento(UUID eventoId, EstadoTarea estado, UUID responsableId) {
        List<TareaEntity> filas;
        if (estado != null && responsableId != null) {
            filas = jpa.findByEventoIdAndEstadoAndResponsableId(eventoId, estado, responsableId);
        } else if (estado != null) {
            filas = jpa.findByEventoIdAndEstado(eventoId, estado);
        } else if (responsableId != null) {
            filas = jpa.findByEventoIdAndResponsableId(eventoId, responsableId);
        } else {
            filas = jpa.findByEventoId(eventoId);
        }
        return filas.stream().map(this::aDominio).sorted(ORDEN).toList();
    }

    @Override
    public Optional<Tarea> buscarEnEvento(UUID tareaId, UUID eventoId) {
        return jpa.findByIdAndEventoId(tareaId, eventoId).map(this::aDominio);
    }

    @Override
    public void eliminar(UUID tareaId) {
        jpa.deleteById(tareaId);
    }

    @Override
    public List<Tarea> listarPendientesQueVencenHasta(LocalDate hasta, LocalDate hoy) {
        return jpa.pendientesQueVencenHasta(EstadoTarea.PENDIENTE, hasta, hoy).stream()
                .map(this::aDominio).toList();
    }

    private TareaEntity aEntity(Tarea t) {
        return new TareaEntity(t.id(), t.eventoId(), t.titulo(), t.descripcion(), t.fechaLimite(), t.estado(),
                t.responsableId(), t.origen(), t.creadoPor(), t.creadoEn(), t.actualizadoEn(), t.completadaEn());
    }

    private Tarea aDominio(TareaEntity e) {
        return new Tarea(e.getId(), e.getEventoId(), e.getTitulo(), e.getDescripcion(), e.getFechaLimite(),
                e.getEstado(), e.getResponsableId(), e.getOrigen(), e.getCreadoPor(), e.getCreadoEn(),
                e.getActualizadoEn(), e.getCompletadaEn());
    }
}
