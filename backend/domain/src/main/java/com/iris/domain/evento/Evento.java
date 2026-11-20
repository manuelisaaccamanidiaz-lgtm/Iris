package com.iris.domain.evento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Un evento (convención, congreso, feria...). Siempre tiene dueño: usuarioId.
 *
 * @param id null mientras no se ha guardado
 */
public record Evento(
        UUID id,
        UUID usuarioId,
        String nombre,
        String descripcion,
        String lugar,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Instant creadoEn) {

    public Evento {
        if (usuarioId == null) {
            throw new ReglaDeNegocioException("El evento debe tener un dueño");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre del evento es obligatorio");
        }
        nombre = nombre.trim();
        if (nombre.length() > 150) {
            throw new ReglaDeNegocioException("El nombre no puede superar 150 caracteres");
        }
        lugar = (lugar == null || lugar.isBlank()) ? null : lugar.trim();
        if (lugar != null && lugar.length() > 200) {
            throw new ReglaDeNegocioException("El lugar no puede superar 200 caracteres");
        }
        descripcion = (descripcion == null || descripcion.isBlank()) ? null : descripcion.trim();
        if (fechaInicio == null || fechaFin == null) {
            throw new ReglaDeNegocioException("Las fechas de inicio y fin son obligatorias");
        }
        // Misma regla que el CHECK ck_eventos_fechas de la BD: doble protección
        if (fechaFin.isBefore(fechaInicio)) {
            throw new ReglaDeNegocioException("La fecha de fin no puede ser anterior a la de inicio");
        }
        if (creadoEn == null) {
            throw new ReglaDeNegocioException("Falta la fecha de creación");
        }
    }

    public static Evento crear(UUID usuarioId, String nombre, String descripcion, String lugar,
                               LocalDate fechaInicio, LocalDate fechaFin) {
        return new Evento(null, usuarioId, nombre, descripcion, lugar, fechaInicio, fechaFin, Instant.now());
    }
}
