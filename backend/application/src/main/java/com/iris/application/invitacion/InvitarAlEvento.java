package com.iris.application.invitacion;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.application.notificacion.Avisos;
import com.iris.application.port.out.Transaccion;
import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.MiembroRepositorio;
import com.iris.domain.evento.RolEnEvento;
import com.iris.domain.invitacion.Invitacion;
import com.iris.domain.invitacion.InvitacionRepositorio;
import com.iris.domain.notificacion.TipoNotificacion;
import com.iris.domain.usuario.Email;
import com.iris.domain.usuario.Usuario;
import com.iris.domain.usuario.UsuarioRepositorio;

/** Caso de uso: un propietario invita a alguien (tenga cuenta o no) por email. */
public class InvitarAlEvento {

    private final AccesoEvento acceso;
    private final MiembroRepositorio miembros;
    private final InvitacionRepositorio invitaciones;
    private final Transaccion transaccion;
    private final UsuarioRepositorio usuarios;
    private final Avisos avisos;

    public InvitarAlEvento(AccesoEvento acceso, MiembroRepositorio miembros, InvitacionRepositorio invitaciones,
                           Transaccion transaccion, UsuarioRepositorio usuarios, Avisos avisos) {
        this.acceso = acceso;
        this.miembros = miembros;
        this.invitaciones = invitaciones;
        this.transaccion = transaccion;
        this.usuarios = usuarios;
        this.avisos = avisos;
    }

    public Invitacion ejecutar(UUID yo, UUID eventoId, String email, RolEnEvento rol) {
        // 1) Guardas: miembro (404) -> propietario (403) -> como PROPIETARIO solo el creador (403)
        if (rol == null) {
            throw new ReglaDeNegocioException("El rol es obligatorio (EDITOR, LECTOR o PROPIETARIO)");
        }
        EventoDeMiembro evento = acceso.comoMiembro(eventoId, yo);
        evento.exigirPuedeInvitarComo(rol, yo);
        Email destino = new Email(email); // 400 si el formato es inválido

        // 2) ¿Ya está en el equipo? (también cubre "invitarme a mí mismo")
        if (miembros.existeConEmail(eventoId, destino.normalizado())) {
            throw new ConflictoException("Esa persona ya es miembro del evento");
        }

        // 3) Todo o nada: vencer la pendiente vieja (si la hay) + crear la nueva
        Invitacion creada = transaccion.ejecutar(() -> {
            Instant ahora = Instant.now();
            Optional<Invitacion> pendiente = invitaciones.buscarPendiente(eventoId, destino.normalizado());
            if (pendiente.isPresent()) {
                if (!pendiente.get().estaVencida(ahora)) {
                    throw new ConflictoException("Ya hay una invitación pendiente para ese email");
                }
                // Venció pero seguía PENDIENTE: si no la cerramos, el índice parcial
                // ux_invitaciones_una_pendiente bloquearía la nueva invitación
                invitaciones.guardar(pendiente.get().vencer(ahora));
            }
            return invitaciones.guardar(Invitacion.crear(eventoId, destino, rol, yo));
        });

        // 4) Avisar DESPUÉS del COMMIT: nunca un correo de una invitación que terminó en ROLLBACK.
        //    Si ya tiene cuenta -> campanita + correo; si no -> solo correo (la verá al registrarse)
        UUID idInvitado = usuarios.buscarPorEmail(destino).map(Usuario::id).orElse(null);
        avisos.aPersona(idInvitado, destino.valor(), eventoId, TipoNotificacion.INVITACION_RECIBIDA,
                "Te invitaron a «" + evento.evento().nombre() + "» como " + rol
                        + ". Entra a Iris para aceptar o rechazar (vence en 7 días)", creada.id());
        return creada;
    }
}
