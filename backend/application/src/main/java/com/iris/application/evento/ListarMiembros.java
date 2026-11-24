package com.iris.application.evento;

import java.util.List;
import java.util.UUID;

import com.iris.domain.evento.MiembroEvento;
import com.iris.domain.evento.MiembroRepositorio;

/** Caso de uso: cualquier miembro ve el equipo del evento. */
public class ListarMiembros {

    private final AccesoEvento acceso;
    private final MiembroRepositorio miembros;

    public ListarMiembros(AccesoEvento acceso, MiembroRepositorio miembros) {
        this.acceso = acceso;
        this.miembros = miembros;
    }

    public List<MiembroEvento> ejecutar(UUID yo, UUID eventoId) {
        acceso.comoMiembro(eventoId, yo);
        return miembros.listar(eventoId);
    }
}
