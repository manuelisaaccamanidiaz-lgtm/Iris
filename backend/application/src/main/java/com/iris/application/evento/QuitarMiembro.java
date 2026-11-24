package com.iris.application.evento;

import java.util.UUID;

import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.MiembroEvento;
import com.iris.domain.evento.MiembroRepositorio;

/** Caso de uso: un propietario saca a alguien del evento. Sus tareas quedan sin responsable (FK de la V9). */
public class QuitarMiembro {

    private final AccesoEvento acceso;
    private final MiembroRepositorio miembros;

    public QuitarMiembro(AccesoEvento acceso, MiembroRepositorio miembros) {
        this.acceso = acceso;
        this.miembros = miembros;
    }

    public void ejecutar(UUID yo, UUID eventoId, UUID usuarioId) {
        EventoDeMiembro evento = acceso.comoMiembro(eventoId, yo);
        evento.exigirAdministracion();
        MiembroEvento objetivo = miembros.buscar(eventoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Miembro no encontrado"));
        evento.exigirPuedeGestionar(objetivo, yo);
        miembros.quitar(eventoId, usuarioId);
    }
}
