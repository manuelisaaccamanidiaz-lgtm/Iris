package com.iris.domain.agenda;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Un pendiente del evento. Puede tener fecha límite y un responsable (miembro del evento).
 * Inmutable: cada cambio devuelve una Tarea nueva.
 */
public record Tarea(
        UUID id,
        UUID eventoId,
        String titulo,
        String descripcion,
        LocalDate fechaLimite,
        EstadoTarea estado,
        UUID responsableId,
        OrigenActividad origen,
        UUID creadoPor,
        Instant creadoEn,
        Instant actualizadoEn,
        Instant completadaEn) {

    public Tarea {
        if (eventoId == null || estado == null || origen == null || creadoEn == null || actualizadoEn == null) {
            throw new ReglaDeNegocioException("Faltan datos obligatorios de la tarea");
        }
        titulo = Sesion.obligatorio(titulo, "El título de la tarea es obligatorio", 200);
        descripcion = Sesion.opcional(descripcion, 5000);
        // Misma regla que el CHECK ck_tareas_completada
        if ((estado == EstadoTarea.HECHA) != (completadaEn != null)) {
            throw new ReglaDeNegocioException("Una tarea hecha debe tener fecha de completado (y una pendiente no)");
        }
    }

    /** Datos que el usuario puede escribir (los mismos para crear y para editar). */
    public record Datos(String titulo, String descripcion, LocalDate fechaLimite, UUID responsableId) {
    }

    public static Tarea crear(UUID eventoId, Datos d, OrigenActividad origen, UUID creadoPor) {
        Instant ahora = Instant.now();
        return new Tarea(null, eventoId, d.titulo(), d.descripcion(), d.fechaLimite(), EstadoTarea.PENDIENTE,
                d.responsableId(), origen, creadoPor, ahora, ahora, null);
    }

    public Tarea editar(Datos d) {
        return new Tarea(id, eventoId, d.titulo(), d.descripcion(), d.fechaLimite(), estado,
                d.responsableId(), origen, creadoPor, creadoEn, Instant.now(), completadaEn);
    }

    public Tarea marcarHecha() {
        if (estado == EstadoTarea.HECHA) {
            return this; // ya estaba hecha: no cambiamos la fecha de completado
        }
        Instant ahora = Instant.now();
        return new Tarea(id, eventoId, titulo, descripcion, fechaLimite, EstadoTarea.HECHA,
                responsableId, origen, creadoPor, creadoEn, ahora, ahora);
    }

    public Tarea reabrir() {
        return new Tarea(id, eventoId, titulo, descripcion, fechaLimite, EstadoTarea.PENDIENTE,
                responsableId, origen, creadoPor, creadoEn, Instant.now(), null);
    }

    public boolean esResponsable(UUID usuarioId) {
        return responsableId != null && responsableId.equals(usuarioId);
    }
}
