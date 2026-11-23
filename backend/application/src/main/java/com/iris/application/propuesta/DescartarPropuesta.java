package com.iris.application.propuesta;

import java.time.Instant;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.propuesta.PropuestaRepositorio;

/** Caso de uso: descartar una propuesta. No se borra: queda DESCARTADA (trazabilidad, como las invitaciones). */
public class DescartarPropuesta {

    private final AccesoEvento acceso;
    private final PropuestaRepositorio propuestas;

    public DescartarPropuesta(AccesoEvento acceso, PropuestaRepositorio propuestas) {
        this.acceso = acceso;
        this.propuestas = propuestas;
    }

    public void ejecutar(UUID yo, UUID eventoId, UUID propuestaId) {
        acceso.comoEditor(eventoId, yo);
        var propuesta = propuestas.buscarEnEvento(propuestaId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propuesta no encontrada"));
        propuestas.guardar(propuesta.descartar(yo, Instant.now())); // 409 si ya no estaba pendiente
    }
}
