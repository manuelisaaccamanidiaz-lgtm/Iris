package com.iris.application.evento;

import java.util.UUID;

import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.EventoRepositorio;

/**
 * Ayudante compartido por los casos de uso: "dame este evento COMO este usuario".
 * Centraliza la regla 404 (no eres miembro) para no repetirla en cada caso de uso.
 */
public class AccesoEvento {

    private final EventoRepositorio eventos;

    public AccesoEvento(EventoRepositorio eventos) {
        this.eventos = eventos;
    }

    /** El evento visto por el usuario (con su rol). 404 si no es miembro. */
    public EventoDeMiembro comoMiembro(UUID eventoId, UUID usuarioId) {
        return eventos.buscarComoMiembro(eventoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
    }

    /** Como comoMiembro, pero además exige rol de edición (403 si es LECTOR). */
    public EventoDeMiembro comoEditor(UUID eventoId, UUID usuarioId) {
        EventoDeMiembro evento = comoMiembro(eventoId, usuarioId);
        evento.exigirEdicion();
        return evento;
    }

    /** Valida que una persona (por ejemplo, el responsable de una tarea) sea miembro del evento. */
    public void exigirQueSeaMiembro(UUID eventoId, UUID usuarioId, String mensaje) {
        if (usuarioId != null && eventos.buscarComoMiembro(eventoId, usuarioId).isEmpty()) {
            throw new ReglaDeNegocioException(mensaje);
        }
    }
}
