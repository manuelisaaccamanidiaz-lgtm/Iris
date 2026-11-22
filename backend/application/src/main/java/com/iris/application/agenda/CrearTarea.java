package com.iris.application.agenda;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.agenda.OrigenActividad;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;

/** Caso de uso: un EDITOR o PROPIETARIO crea una tarea (opcionalmente con responsable). */
public class CrearTarea {

    private final AccesoEvento acceso;
    private final TareaRepositorio tareas;

    public CrearTarea(AccesoEvento acceso, TareaRepositorio tareas) {
        this.acceso = acceso;
        this.tareas = tareas;
    }

    public Tarea ejecutar(UUID usuarioId, UUID eventoId, Tarea.Datos datos) {
        acceso.comoEditor(eventoId, usuarioId);
        // Mensaje claro en el código; la FK compuesta de la V9 es el último muro
        acceso.exigirQueSeaMiembro(eventoId, datos.responsableId(), "El responsable debe ser miembro del evento");
        return tareas.guardar(Tarea.crear(eventoId, datos, OrigenActividad.MANUAL, usuarioId));
    }
}
