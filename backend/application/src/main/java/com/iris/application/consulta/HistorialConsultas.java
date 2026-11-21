package com.iris.application.consulta;

import java.util.List;
import java.util.UUID;

import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.consulta.Consulta;
import com.iris.domain.consulta.ConsultaRepositorio;
import com.iris.domain.evento.EventoRepositorio;

/** Caso de uso: ver MIS preguntas anteriores en un evento (privacidad: no las de los demás). */
public class HistorialConsultas {

    private static final int LIMITE = 50;

    private final EventoRepositorio eventos;
    private final ConsultaRepositorio consultas;

    public HistorialConsultas(EventoRepositorio eventos, ConsultaRepositorio consultas) {
        this.eventos = eventos;
        this.consultas = consultas;
    }

    public List<Consulta> ejecutar(UUID usuarioId, UUID eventoId) {
        eventos.buscarComoMiembro(eventoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
        return consultas.listarDelUsuario(eventoId, usuarioId, LIMITE);
    }
}
