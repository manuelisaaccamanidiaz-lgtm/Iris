package com.iris.application.notificacion;

import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.UUID;

import com.iris.application.port.out.Notificador;
import com.iris.application.port.out.Notificador.Aviso;
import com.iris.domain.evento.MiembroEvento;
import com.iris.domain.evento.MiembroRepositorio;
import com.iris.domain.evento.RolEnEvento;
import com.iris.domain.notificacion.TipoNotificacion;

/**
 * Ayudante compartido: "¿a QUIÉN aviso?". Resuelve los destinatarios (los propietarios,
 * el responsable, un email) y le pasa cada aviso al puerto Notificador.
 *
 * <p>TOLERANCIA A FALLOS: avisar es un extra. Si algo falla aquí (la BD de notificaciones,
 * el correo...), se registra en el log y el caso de uso sigue: la tarea, la sesión o la
 * invitación ya se guardaron y eso es lo importante.
 */
public class Avisos {

    // Logger de Java puro (java.lang.System.Logger): application sigue sin depender de Spring ni de SLF4J
    private static final System.Logger LOG = System.getLogger(Avisos.class.getName());

    private final MiembroRepositorio miembros;
    private final Notificador notificador;

    public Avisos(MiembroRepositorio miembros, Notificador notificador) {
        this.miembros = miembros;
        this.notificador = notificador;
    }

    /** A todos los propietarios del evento, menos a quienes se indique (quien hizo la acción, el responsable...). */
    public void aPropietarios(UUID eventoId, TipoNotificacion tipo, String mensaje, UUID referenciaId,
                              UUID... excepto) {
        seguro(() -> miembros.listar(eventoId).stream()
                .filter(m -> m.rol() == RolEnEvento.PROPIETARIO)
                .filter(m -> Arrays.stream(excepto).noneMatch(m.usuarioId()::equals))
                .forEach(m -> enviar(m, tipo, mensaje, referenciaId)));
    }

    /** A un miembro concreto (por ejemplo, el responsable de una tarea). Si ya no es miembro, a nadie. */
    public void aMiembro(UUID eventoId, UUID usuarioId, TipoNotificacion tipo, String mensaje, UUID referenciaId) {
        seguro(() -> miembros.buscar(eventoId, usuarioId)
                .ifPresent(m -> enviar(m, tipo, mensaje, referenciaId)));
    }

    /** A alguien que quizá aún no es miembro ni tiene cuenta (el invitado). */
    public void aPersona(UUID usuarioIdONull, String email, UUID eventoId, TipoNotificacion tipo,
                         String mensaje, UUID referenciaId) {
        seguro(() -> notificador.notificar(new Aviso(usuarioIdONull, email, eventoId, tipo, mensaje, referenciaId)));
    }

    private void enviar(MiembroEvento m, TipoNotificacion tipo, String mensaje, UUID referenciaId) {
        notificador.notificar(new Aviso(m.usuarioId(), m.email(), m.eventoId(), tipo, mensaje, referenciaId));
    }

    private static void seguro(Runnable trabajo) {
        try {
            trabajo.run();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudo enviar un aviso: " + e.getMessage(), e);
        }
    }
}
