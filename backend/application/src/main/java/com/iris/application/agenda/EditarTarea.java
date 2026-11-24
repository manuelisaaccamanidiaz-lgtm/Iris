package com.iris.application.agenda;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.application.notificacion.Avisos;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.notificacion.TipoNotificacion;

/** Caso de uso: cambiar título, descripción, fecha límite o responsable (EDITOR o PROPIETARIO). */
public class EditarTarea {

    private final AccesoEvento acceso;
    private final TareaRepositorio tareas;
    private final Avisos avisos;

    public EditarTarea(AccesoEvento acceso, TareaRepositorio tareas, Avisos avisos) {
        this.acceso = acceso;
        this.tareas = tareas;
        this.avisos = avisos;
    }

    public Tarea ejecutar(UUID usuarioId, UUID eventoId, UUID tareaId, Tarea.Datos datos) {
        EventoDeMiembro evento = acceso.comoEditor(eventoId, usuarioId);
        acceso.exigirQueSeaMiembro(eventoId, datos.responsableId(), "El responsable debe ser miembro del evento");
        Tarea actual = tareas.buscarEnEvento(tareaId, eventoId)
                .filter(t -> t.visiblePara(usuarioId, evento.miRol()))   // si no la puede ver, "no existe"
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));
        Tarea editada = tareas.guardar(actual.editar(datos));

        // ¿Cambió de responsable? Avisamos al NUEVO (si no se la asignó él mismo)
        UUID nuevo = editada.responsableId();
        if (nuevo != null && !nuevo.equals(actual.responsableId()) && !nuevo.equals(usuarioId)) {
            avisos.aMiembro(eventoId, nuevo, TipoNotificacion.TAREA_ASIGNADA,
                    "Te asignaron la tarea «" + editada.titulo() + "» en «" + evento.evento().nombre() + "»",
                    editada.id());
        }
        return editada;
    }
}
