package com.iris.application.evento;

import java.time.LocalDate;
import java.util.UUID;

import com.iris.domain.evento.Evento;
import com.iris.domain.evento.EventoRepositorio;

/** Caso de uso: un organizador crea un evento propio. */
public class CrearEvento {

    private final EventoRepositorio eventos;

    public CrearEvento(EventoRepositorio eventos) {
        this.eventos = eventos;
    }

    /** usuarioId viene del token (lo pone el controlador), nunca del JSON. */
    public Evento ejecutar(UUID usuarioId, Comando c) {
        return eventos.guardar(Evento.crear(usuarioId, c.nombre(), c.descripcion(), c.lugar(),
                c.fechaInicio(), c.fechaFin()));
    }

    public record Comando(String nombre, String descripcion, String lugar,
                          LocalDate fechaInicio, LocalDate fechaFin) {
    }
}
