package com.iris.application.propuesta;

import java.util.List;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.domain.propuesta.EstadoPropuesta;
import com.iris.domain.propuesta.Propuesta;
import com.iris.domain.propuesta.PropuestaRepositorio;

/** Caso de uso: ver las propuestas del evento (decisión de Isaac: EDITOR o PROPIETARIO; un LECTOR no). */
public class ListarPropuestas {

    private final AccesoEvento acceso;
    private final PropuestaRepositorio propuestas;

    public ListarPropuestas(AccesoEvento acceso, PropuestaRepositorio propuestas) {
        this.acceso = acceso;
        this.propuestas = propuestas;
    }

    /** @param estado null = todas */
    public List<Propuesta> ejecutar(UUID yo, UUID eventoId, EstadoPropuesta estado) {
        acceso.comoEditor(eventoId, yo);
        return propuestas.listarPorEvento(eventoId, estado);
    }
}
