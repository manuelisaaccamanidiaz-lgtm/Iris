package com.iris.application.invitacion;

import java.time.Instant;
import java.util.UUID;

import com.iris.application.notificacion.Avisos;
import com.iris.application.port.out.Transaccion;
import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.evento.MiembroRepositorio;
import com.iris.domain.invitacion.Invitacion;
import com.iris.domain.invitacion.InvitacionRepositorio;
import com.iris.domain.notificacion.TipoNotificacion;

/** Caso de uso: acepto una invitación dirigida a mi email y entro al equipo con su rol. */
public class AceptarInvitacion {

    private final InvitacionesDelUsuario delUsuario;
    private final InvitacionRepositorio invitaciones;
    private final MiembroRepositorio miembros;
    private final Transaccion transaccion;
    private final Avisos avisos;

    public AceptarInvitacion(InvitacionesDelUsuario delUsuario, InvitacionRepositorio invitaciones,
                             MiembroRepositorio miembros, Transaccion transaccion, Avisos avisos) {
        this.delUsuario = delUsuario;
        this.invitaciones = invitaciones;
        this.miembros = miembros;
        this.transaccion = transaccion;
        this.avisos = avisos;
    }

    public Invitacion ejecutar(UUID yo, UUID invitacionId) {
        Invitacion invitacion = delUsuario.buscarMia(yo, invitacionId); // 404 si no es para mí
        Instant ahora = Instant.now();

        // LA TRAMPA: si venció, queremos GUARDARLA como VENCIDA y además responder 409.
        // Si lanzáramos la excepción DENTRO de la transacción, el ROLLBACK desharía el guardado.
        // Por eso: primero se guarda (la transacción termina con COMMIT) y DESPUÉS se lanza.
        if (invitacion.estaVencida(ahora)) {
            transaccion.ejecutarSinResultado(() -> invitaciones.guardar(invitacion.vencer(ahora)));
            throw new ConflictoException("Esta invitación venció; pide que te inviten de nuevo");
        }

        // Dos tablas, una transacción: o pasan las dos escrituras o no pasa ninguna
        Invitacion aceptada = transaccion.ejecutar(() -> {
            if (miembros.buscar(invitacion.eventoId(), yo).isPresent()) {
                throw new ConflictoException("Ya eres miembro de este evento");
            }
            Invitacion cerrada = invitaciones.guardar(invitacion.aceptar(ahora)); // 409 si ya no estaba pendiente
            miembros.agregar(invitacion.eventoId(), yo, invitacion.rol());
            return cerrada;
        });

        // Después del COMMIT: los propietarios se enteran de quién entró (menos el propio invitado)
        avisos.aPropietarios(invitacion.eventoId(), TipoNotificacion.MIEMBRO_NUEVO,
                delUsuario.yo(yo).nombre() + " se unió al evento como " + invitacion.rol(), null, yo);
        return aceptada;
    }
}
