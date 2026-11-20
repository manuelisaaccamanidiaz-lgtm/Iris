package com.iris.infrastructure.adapter.in.web.evento;

import java.time.LocalDate;
import java.util.UUID;

import com.iris.domain.evento.Evento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

final class EventoDtos {

    private EventoDtos() {
    }

    /** Sin usuarioId: el dueño SIEMPRE sale del token. Fechas en formato "2026-05-20". */
    record SolicitudEvento(@NotBlank String nombre, String descripcion, String lugar,
                           @NotNull LocalDate fechaInicio, @NotNull LocalDate fechaFin) {
    }

    record RespuestaEvento(UUID id, String nombre, String descripcion, String lugar,
                           LocalDate fechaInicio, LocalDate fechaFin) {

        static RespuestaEvento de(Evento e) {
            return new RespuestaEvento(e.id(), e.nombre(), e.descripcion(), e.lugar(),
                    e.fechaInicio(), e.fechaFin());
        }
    }
}
