package com.iris.application.agenda;

import java.util.List;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.agenda.EstadoTarea;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;

/** Caso de uso: ver las tareas del evento, con filtros opcionales. Cualquier miembro. */
public class ListarTareas {

    private final AccesoEvento acceso;
    private final TareaRepositorio tareas;

    public ListarTareas(AccesoEvento acceso, TareaRepositorio tareas) {
        this.acceso = acceso;
        this.tareas = tareas;
    }

    /**
     * @param estado   null = todas
     * @param soloMias true = solo las que tienen como responsable al usuario que pregunta
     */
    public List<Tarea> ejecutar(UUID usuarioId, UUID eventoId, EstadoTarea estado, boolean soloMias) {
        acceso.comoMiembro(eventoId, usuarioId);
        UUID responsable = soloMias ? usuarioId : null;
        return tareas.listarPorEvento(eventoId, estado, responsable);
    }
}
