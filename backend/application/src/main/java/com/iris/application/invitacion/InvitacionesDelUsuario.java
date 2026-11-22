package com.iris.application.invitacion;

import java.util.UUID;

import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.invitacion.Invitacion;
import com.iris.domain.invitacion.InvitacionRepositorio;
import com.iris.domain.usuario.Email;
import com.iris.domain.usuario.UsuarioRepositorio;

/**
 * Ayudante compartido (como AccesoEvento): "dame esta invitación SI es para mí".
 * El invitado todavía no es miembro del evento, así que la regla no es la membresía
 * sino el EMAIL: la invitación debe ir dirigida al email de quien hace la petición.
 */
public class InvitacionesDelUsuario {

    private final UsuarioRepositorio usuarios;
    private final InvitacionRepositorio invitaciones;

    public InvitacionesDelUsuario(UsuarioRepositorio usuarios, InvitacionRepositorio invitaciones) {
        this.usuarios = usuarios;
        this.invitaciones = invitaciones;
    }

    /** El email de quien hace la petición (el JWT solo trae su id). */
    public Email miEmail(UUID yo) {
        return usuarios.buscarPorId(yo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"))
                .email();
    }

    /** 404 si no existe O si no es para mí: no revelamos invitaciones ajenas. */
    public Invitacion buscarMia(UUID yo, UUID invitacionId) {
        Email mio = miEmail(yo);
        return invitaciones.buscarPorId(invitacionId)
                .filter(inv -> inv.esPara(mio))
                .orElseThrow(() -> new RecursoNoEncontradoException("Invitación no encontrada"));
    }
}
