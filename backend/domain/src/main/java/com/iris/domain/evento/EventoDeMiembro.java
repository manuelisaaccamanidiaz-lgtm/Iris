package com.iris.domain.evento;

import java.time.LocalDate;
import java.util.UUID;

import com.iris.domain.compartido.AccesoDenegadoException;
import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Un evento visto POR un miembro: el evento + el rol que esa persona tiene en él.
 * Es lo que devuelve toda búsqueda: nunca obtienes un evento sin saber tu rol.
 *
 * <p>Aquí viven las cláusulas de guarda de permisos. Desde el 4.2 puede haber VARIOS
 * propietarios, pero el CREADOR (evento.creadoPor) es el propietario original: solo él
 * nombra, cambia o quita propietarios, y nadie lo puede quitar a él.
 */
public record EventoDeMiembro(Evento evento, RolEnEvento miRol) {

    /** Lanza 403 si mi rol no permite editar (subir documentos, crear tareas...). */
    public void exigirEdicion() {
        if (!miRol.puedeEditar()) {
            throw new AccesoDenegadoException("Tu rol en este evento (" + miRol + ") no permite modificarlo");
        }
    }

    /** Lanza 403 si no soy propietario (invitar, cambiar roles, quitar miembros...). */
    public void exigirAdministracion() {
        if (!miRol.puedeAdministrar()) {
            throw new AccesoDenegadoException("Solo un propietario puede hacer esto");
        }
    }

    /** ¿Soy quien creó el evento (el propietario original)? */
    public boolean esCreador(UUID yo) {
        return evento.creadoPor().equals(yo);
    }

    /** Lanza 403 si no soy el creador. Nombrar o tocar propietarios es solo suyo (decisión 3b). */
    public void exigirCreador(UUID yo) {
        if (!esCreador(yo)) {
            throw new AccesoDenegadoException("Solo el creador del evento puede nombrar o gestionar propietarios");
        }
    }

    /** Invitar: cualquier propietario; invitar COMO propietario, solo el creador (decisión 3a). */
    public void exigirPuedeInvitarComo(RolEnEvento rol, UUID yo) {
        exigirAdministracion();
        if (rol == RolEnEvento.PROPIETARIO) {
            exigirCreador(yo);
        }
    }

    /**
     * Quitar a otro miembro o cambiarle el rol.
     * <ul>
     *   <li>403 si no soy propietario.</li>
     *   <li>400 si el objetivo soy yo mismo (para irme está "salir").</li>
     *   <li>400 si el objetivo es el creador: nadie lo toca, así el evento nunca queda huérfano.</li>
     *   <li>403 si el objetivo es un propietario y yo no soy el creador.</li>
     * </ul>
     */
    public void exigirPuedeGestionar(MiembroEvento objetivo, UUID yo) {
        exigirAdministracion();
        if (objetivo.usuarioId().equals(yo)) {
            throw new ReglaDeNegocioException("No puedes cambiar tu propio rol ni quitarte; para irte usa \"salir del evento\"");
        }
        if (esCreador(objetivo.usuarioId())) {
            throw new ReglaDeNegocioException("Al creador del evento no se le puede quitar ni cambiar el rol");
        }
        if (objetivo.rol() == RolEnEvento.PROPIETARIO) {
            exigirCreador(yo);
        }
    }

    /** Igual que gestionar, y además ascender a PROPIETARIO es solo del creador. */
    public void exigirPuedeCambiarRol(MiembroEvento objetivo, RolEnEvento nuevoRol, UUID yo) {
        exigirPuedeGestionar(objetivo, yo);
        if (nuevoRol == RolEnEvento.PROPIETARIO) {
            exigirCreador(yo);
        }
    }

    /** Salir: cualquiera puede; el creador, solo cuando el evento ya terminó (decisión 3c). */
    public void exigirPuedeSalir(UUID yo, LocalDate hoy) {
        if (esCreador(yo) && !evento.terminoAntesDe(hoy)) {
            throw new ReglaDeNegocioException("Como creador, solo puedes salir cuando el evento haya terminado (después del "
                    + evento.fechaFin() + ")");
        }
    }
}
