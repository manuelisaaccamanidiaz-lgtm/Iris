package com.iris.domain.agenda;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.evento.Evento;

/**
 * Una actividad de la agenda del evento (conferencia, taller, reunión...).
 * Inmutable: editar devuelve una Sesion nueva.
 */
public record Sesion(
        UUID id,
        UUID eventoId,
        String titulo,
        String descripcion,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String sala,
        String ponente,
        TipoSesion tipo,
        OrigenActividad origen,
        UUID creadoPor,
        Instant creadoEn,
        Instant actualizadoEn) {

    public Sesion {
        if (eventoId == null || origen == null || creadoEn == null || actualizadoEn == null) {
            throw new ReglaDeNegocioException("Faltan datos obligatorios de la sesión");
        }
        titulo = obligatorio(titulo, "El título de la sesión es obligatorio", 200);
        descripcion = opcional(descripcion, 5000);
        sala = opcional(sala, 100);
        ponente = opcional(ponente, 150);
        tipo = tipo == null ? TipoSesion.CONFERENCIA : tipo;
        if (fecha == null || horaInicio == null || horaFin == null) {
            throw new ReglaDeNegocioException("La fecha y las horas de la sesión son obligatorias");
        }
        // Misma regla que el CHECK ck_sesiones_horas: doble protección
        if (!horaFin.isAfter(horaInicio)) {
            throw new ReglaDeNegocioException("La hora de fin debe ser posterior a la de inicio");
        }
    }

    /** Datos que el usuario puede escribir (los mismos para crear y para editar). */
    public record Datos(String titulo, String descripcion, LocalDate fecha, LocalTime horaInicio,
                        LocalTime horaFin, String sala, String ponente, TipoSesion tipo) {
    }

    /**
     * Crea una sesión validando contra el EVENTO: la fecha debe caer dentro de sus días.
     * Recibe el Evento completo para poder aplicar esa regla aquí, en el dominio.
     */
    public static Sesion crear(Evento evento, Datos d, OrigenActividad origen, UUID creadoPor) {
        validarFechaEnEvento(evento, d.fecha());
        Instant ahora = Instant.now();
        return new Sesion(null, evento.id(), d.titulo(), d.descripcion(), d.fecha(), d.horaInicio(),
                d.horaFin(), d.sala(), d.ponente(), d.tipo(), origen, creadoPor, ahora, ahora);
    }

    public Sesion editar(Evento evento, Datos d) {
        validarFechaEnEvento(evento, d.fecha());
        return new Sesion(id, eventoId, d.titulo(), d.descripcion(), d.fecha(), d.horaInicio(),
                d.horaFin(), d.sala(), d.ponente(), d.tipo(), origen, creadoPor, creadoEn, Instant.now());
    }

    private static void validarFechaEnEvento(Evento evento, LocalDate fecha) {
        if (fecha != null && (fecha.isBefore(evento.fechaInicio()) || fecha.isAfter(evento.fechaFin()))) {
            throw new ReglaDeNegocioException("La sesión debe estar entre el " + evento.fechaInicio()
                    + " y el " + evento.fechaFin() + " (las fechas del evento)");
        }
    }

    static String obligatorio(String valor, String mensaje, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDeNegocioException(mensaje);
        }
        String limpio = valor.strip();
        if (limpio.length() > maximo) {
            throw new ReglaDeNegocioException("El texto no puede superar " + maximo + " caracteres");
        }
        return limpio;
    }

    static String opcional(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.strip();
        if (limpio.length() > maximo) {
            throw new ReglaDeNegocioException("El texto no puede superar " + maximo + " caracteres");
        }
        return limpio;
    }
}
