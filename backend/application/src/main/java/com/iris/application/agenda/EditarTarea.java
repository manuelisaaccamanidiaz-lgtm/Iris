package com.iris.application.agenda;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;
import com.iris.domain.compartido.RecursoNoEncontradoException;

/** Caso de uso: cambiar título, descripción, fecha límite o responsable (EDITOR o PROPIETARIO). */
public class EditarTarea {

    private final AccesoEvento acceso;
    private final TareaRepositorio tareas;

    public EditarTarea(AccesoEvento acceso, TareaRepositorio tareas) {
        this.acceso = acceso;
        this.tareas = tareas;
    }

    public Tarea ejecutar(UUID usuarioId, UUID eventoId, UUID tareaId, Tarea.Datos datos) {
        acceso.comoEditor(eventoId, usuarioId);
        acceso.exigirQueSeaMiembro(eventoId, datos.responsableId(), "El responsable debe ser miembro del evento");
        Tarea actual = tareas.buscarEnEvento(tareaId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));
        return tareas.guardar(actual.editar(datos));
    }
}
