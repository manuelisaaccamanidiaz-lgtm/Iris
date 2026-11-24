package com.iris.application.propuesta;

import java.util.List;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;

/** Caso de uso: "¿qué documentos de este evento está leyendo Iris ahora?" (EDITOR o PROPIETARIO, igual que revisar). */
public class RevisionesEnCurso {

    private final AccesoEvento acceso;
    private final GenerarPropuestas generar;

    public RevisionesEnCurso(AccesoEvento acceso, GenerarPropuestas generar) {
        this.acceso = acceso;
        this.generar = generar;
    }

    public List<UUID> ejecutar(UUID yo, UUID eventoId) {
        acceso.comoEditor(eventoId, yo); // 403 a un lector, 404 a quien no es del evento
        return generar.enRevisionDelEvento(eventoId);
    }
}
