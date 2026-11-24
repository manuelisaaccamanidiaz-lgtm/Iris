package com.iris.application.propuesta;

import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.propuesta.EstadoPropuesta;
import com.iris.domain.propuesta.Propuesta;
import com.iris.domain.propuesta.PropuestaRepositorio;

/** Caso de uso (Día 6, decisión de Isaac): devolver una propuesta DESCARTADA a "Por revisar". */
public class RecuperarPropuesta {

    private final AccesoEvento acceso;
    private final PropuestaRepositorio propuestas;

    public RecuperarPropuesta(AccesoEvento acceso, PropuestaRepositorio propuestas) {
        this.acceso = acceso;
        this.propuestas = propuestas;
    }

    public Propuesta ejecutar(UUID yo, UUID eventoId, UUID propuestaId) {
        acceso.comoEditor(eventoId, yo);
        Propuesta actual = propuestas.buscarEnEvento(propuestaId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propuesta no encontrada"));
        if (actual.estado() != EstadoPropuesta.DESCARTADA || !propuestas.recuperarDescartada(propuestaId, eventoId)) {
            throw new ConflictoException("Solo se puede recuperar una propuesta descartada");
        }
        return propuestas.buscarEnEvento(propuestaId, eventoId).orElseThrow();
    }
}
