package com.iris.application.agenda;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.agenda.EstadoTarea;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;
import com.iris.domain.compartido.AccesoDenegadoException;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.evento.EventoDeMiembro;

/**
 * Caso de uso: marcar una tarea como hecha o reabrirla.
 * Regla especial: además de EDITOR y PROPIETARIO, puede hacerlo su RESPONSABLE aunque sea LECTOR
 * (quien hizo la tarea debe poder marcarla, aunque no pueda editar el resto del evento).
 */
public class CambiarEstadoTarea {

    private final AccesoEvento acceso;
    private final TareaRepositorio tareas;

    public CambiarEstadoTarea(AccesoEvento acceso, TareaRepositorio tareas) {
        this.acceso = acceso;
        this.tareas = tareas;
    }

    public Tarea ejecutar(UUID usuarioId, UUID eventoId, UUID tareaId, EstadoTarea nuevoEstado) {
        EventoDeMiembro evento = acceso.comoMiembro(eventoId, usuarioId);
        Tarea actual = tareas.buscarEnEvento(tareaId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));

        if (!evento.miRol().puedeEditar() && !actual.esResponsable(usuarioId)) {
            throw new AccesoDenegadoException("Solo un editor o el responsable de la tarea puede cambiar su estado");
        }

        Tarea cambiada = (nuevoEstado == EstadoTarea.HECHA) ? actual.marcarHecha() : actual.reabrir();
        return tareas.guardar(cambiada);
    }
}
