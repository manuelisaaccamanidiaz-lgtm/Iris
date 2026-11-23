package com.iris.application.propuesta;

import java.time.Instant;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.application.notificacion.Avisos;
import com.iris.application.port.out.Transaccion;
import com.iris.domain.agenda.OrigenActividad;
import com.iris.domain.agenda.Sesion;
import com.iris.domain.agenda.SesionRepositorio;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.notificacion.TipoNotificacion;
import com.iris.domain.propuesta.Propuesta;
import com.iris.domain.propuesta.PropuestaRepositorio;
import com.iris.domain.propuesta.TipoPropuesta;

/**
 * Caso de uso: un EDITOR o PROPIETARIO acepta una propuesta, tal cual o con cambios
 * (decisión de Isaac), y se crea la tarea o la sesión con origen IRIS.
 */
public class AceptarPropuesta {

    private final AccesoEvento acceso;
    private final PropuestaRepositorio propuestas;
    private final TareaRepositorio tareas;
    private final SesionRepositorio sesiones;
    private final Transaccion transaccion;
    private final Avisos avisos;

    public AceptarPropuesta(AccesoEvento acceso, PropuestaRepositorio propuestas, TareaRepositorio tareas,
                            SesionRepositorio sesiones, Transaccion transaccion, Avisos avisos) {
        this.acceso = acceso;
        this.propuestas = propuestas;
        this.tareas = tareas;
        this.sesiones = sesiones;
        this.transaccion = transaccion;
        this.avisos = avisos;
    }

    /**
     * @param ajustes       correcciones de la persona; null = tal como lo propuso Iris
     * @param responsableId solo para tareas; null = sin responsable
     * @return la propuesta ya ACEPTADA (su resultadoId es el id de la tarea o sesión creada)
     */
    public Propuesta ejecutar(UUID yo, UUID eventoId, UUID propuestaId, Propuesta.Ajustes ajustes,
                              UUID responsableId) {
        EventoDeMiembro evento = acceso.comoEditor(eventoId, yo);
        Propuesta propuesta = propuestas.buscarEnEvento(propuestaId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propuesta no encontrada"))
                .conAjustes(ajustes); // 409 si ya no está pendiente; 400 si los cambios son inválidos
        if (propuesta.tipo() == TipoPropuesta.TAREA) {
            acceso.exigirQueSeaMiembro(eventoId, responsableId, "El responsable debe ser miembro del evento");
        }

        // Dos tablas, una transacción: crear la tarea/sesión Y marcar la propuesta como aceptada.
        // Si el doble clic hace que el UPDATE condicional falle (409), el ROLLBACK borra la tarea
        // recién creada: nunca quedan dos tareas por una propuesta
        Propuesta aceptada = transaccion.ejecutar(() -> {
            UUID creado;
            if (propuesta.tipo() == TipoPropuesta.TAREA) {
                creado = tareas.guardar(Tarea.crear(eventoId, propuesta.aDatosTarea(responsableId),
                        OrigenActividad.IRIS, yo)).id();
            } else {
                // Sesion.crear vuelve a validar que la fecha esté dentro del evento (400 si no)
                creado = sesiones.guardar(Sesion.crear(evento.evento(), propuesta.aDatosSesion(),
                        OrigenActividad.IRIS, yo)).id();
            }
            return propuestas.guardar(propuesta.aceptar(yo, creado, Instant.now()));
        });

        // Después del COMMIT (lección del 4.3)
        if (responsableId != null && !responsableId.equals(yo)) {
            avisos.aMiembro(eventoId, responsableId, TipoNotificacion.TAREA_ASIGNADA,
                    "Te asignaron la tarea «" + aceptada.titulo() + "» en «" + evento.evento().nombre()
                            + "» (propuesta por Iris desde «" + aceptada.documentoNombre() + "»)",
                    aceptada.resultadoId());
        }
        return aceptada;
    }
}
