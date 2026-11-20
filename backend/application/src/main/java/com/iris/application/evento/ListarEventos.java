package com.iris.application.evento;

import java.util.List;
import java.util.UUID;

import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.EventoRepositorio;

/** Caso de uso: ver los eventos donde soy miembro, con mi rol en cada uno. */
public class ListarEventos {

    private final EventoRepositorio eventos;

    public ListarEventos(EventoRepositorio eventos) {
        this.eventos = eventos;
    }

    public List<EventoDeMiembro> ejecutar(UUID usuarioId) {
        return eventos.listarDeMiembro(usuarioId);
    }
}
