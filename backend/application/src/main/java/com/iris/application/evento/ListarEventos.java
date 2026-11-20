package com.iris.application.evento;

import java.util.List;
import java.util.UUID;

import com.iris.domain.evento.Evento;
import com.iris.domain.evento.EventoRepositorio;

/** Caso de uso: ver mis eventos (solo los míos). */
public class ListarEventos {

    private final EventoRepositorio eventos;

    public ListarEventos(EventoRepositorio eventos) {
        this.eventos = eventos;
    }

    public List<Evento> ejecutar(UUID usuarioId) {
        return eventos.listarPorUsuario(usuarioId);
    }
}
