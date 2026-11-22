package com.iris.application.agenda;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.agenda.TareaRepositorio;
import com.iris.domain.compartido.RecursoNoEncontradoException;

/** Caso de uso: borrar una tarea (EDITOR o PROPIETARIO). */
public class EliminarTarea {

    private final AccesoEvento acceso;
    private final TareaRepositorio tareas;

    public EliminarTarea(AccesoEvento acceso, TareaRepositorio tareas) {
        this.acceso = acceso;
        this.tareas = tareas;
    }

    public void ejecutar(UUID usuarioId, UUID eventoId, UUID tareaId) {
        acceso.comoEditor(eventoId, usuarioId);
        tareas.buscarEnEvento(tareaId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));
        tareas.eliminar(tareaId);
    }
}
