package com.iris.application.evento;

import java.time.LocalDate;
import java.util.UUID;

import com.iris.domain.evento.Evento;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.EventoDuplicadoException;
import com.iris.domain.evento.EventoRepositorio;

/** Caso de uso: crear un evento. Quien lo crea queda como PROPIETARIO. */
public class CrearEvento {

    private final EventoRepositorio eventos;

    public CrearEvento(EventoRepositorio eventos) {
        this.eventos = eventos;
    }

    /** usuarioId viene del token (lo pone el controlador), nunca del JSON. */
    public EventoDeMiembro ejecutar(UUID usuarioId, Comando c) {
        Evento evento = Evento.crear(usuarioId, c.nombre(), c.descripcion(), c.lugar(),
                c.fechaInicio(), c.fechaFin());
        // Mismo patrón que el email: revisamos en el código (mensaje claro) y el índice
        // único de la V7 es el último muro si llegan dos peticiones al mismo tiempo
        if (eventos.existeDelCreador(usuarioId, evento.nombre(), evento.fechaInicio())) {
            throw new EventoDuplicadoException();
        }
        return eventos.crearConPropietario(evento);
    }

    public record Comando(String nombre, String descripcion, String lugar,
                          LocalDate fechaInicio, LocalDate fechaFin) {
    }
}
