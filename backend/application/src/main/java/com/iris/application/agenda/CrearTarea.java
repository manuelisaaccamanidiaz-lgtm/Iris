package com.iris.application.agenda;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.application.notificacion.Avisos;
import com.iris.domain.agenda.OrigenActividad;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.notificacion.TipoNotificacion;

/** Caso de uso: un EDITOR o PROPIETARIO crea una tarea (opcionalmente con responsable). */
public class CrearTarea {

    private final AccesoEvento acceso;
    private final TareaRepositorio tareas;
    private final Avisos avisos;

    public CrearTarea(AccesoEvento acceso, TareaRepositorio tareas, Avisos avisos) {
        this.acceso = acceso;
        this.tareas = tareas;
        this.avisos = avisos;
    }

    public Tarea ejecutar(UUID usuarioId, UUID eventoId, Tarea.Datos datos) {
        EventoDeMiembro evento = acceso.comoEditor(eventoId, usuarioId);
        // Mensaje claro en el código; la FK compuesta de la V9 es el último muro
        acceso.exigirQueSeaMiembro(eventoId, datos.responsableId(), "El responsable debe ser miembro del evento");
        Tarea nueva = tareas.guardar(Tarea.crear(eventoId, datos, OrigenActividad.MANUAL, usuarioId));
        String nombreEvento = evento.evento().nombre();

        // Al responsable (si no se la asignó él mismo)
        if (nueva.responsableId() != null && !nueva.responsableId().equals(usuarioId)) {
            avisos.aMiembro(eventoId, nueva.responsableId(), TipoNotificacion.TAREA_ASIGNADA,
                    "Te asignaron la tarea «" + nueva.titulo() + "» en «" + nombreEvento + "»", nueva.id());
        }
        // A los propietarios, menos a quien la creó y al responsable (que ya recibió su aviso)
        avisos.aPropietarios(eventoId, TipoNotificacion.TAREA_NUEVA,
                "Nueva tarea en «" + nombreEvento + "»: " + nueva.titulo(), nueva.id(),
                usuarioId, nueva.responsableId());
        return nueva;
    }
}
