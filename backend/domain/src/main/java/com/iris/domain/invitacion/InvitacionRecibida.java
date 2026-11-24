package com.iris.domain.invitacion;

import java.time.LocalDate;

/**
 * Lo que ve el invitado en su bandeja: la invitación + datos del evento y de quién lo invitó.
 * (El invitado todavía NO es miembro, así que no puede pedir el evento por su cuenta.)
 *
 * @param invitadoPorNombre null si quien invitó ya borró su cuenta
 */
public record InvitacionRecibida(
        Invitacion invitacion,
        String nombreEvento,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String invitadoPorNombre) {
}
