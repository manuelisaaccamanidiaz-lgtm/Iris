package com.iris.application.agenda;

import java.util.List;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.agenda.EstadoTarea;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.RolEnEvento;

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
     *                 (para quien no es PROPIETARIO esto es siempre así, lo pida o no)
     */
    public List<Tarea> ejecutar(UUID usuarioId, UUID eventoId, EstadoTarea estado, boolean soloMias) {
        EventoDeMiembro evento = acceso.comoMiembro(eventoId, usuarioId);
        boolean esPropietario = evento.miRol() == RolEnEvento.PROPIETARIO;

        // Regla de visibilidad: solo el propietario puede ver las tareas de todos
        UUID responsable = null;
        if (soloMias || !esPropietario) {
            responsable = usuarioId;
        }
        return tareas.listarPorEvento(eventoId, estado, responsable);
    }
}
