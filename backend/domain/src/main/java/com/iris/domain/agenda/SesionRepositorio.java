package com.iris.domain.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida para sesiones. Siempre DENTRO de un evento (quien llama ya verificó la membresía). */
public interface SesionRepositorio {

    Sesion guardar(Sesion sesion);

    /** La agenda en orden cronológico. */
    List<Sesion> listarPorEvento(UUID eventoId);

    /** Vacío si no existe o si es de OTRO evento (así no se puede tocar una sesión ajena por su id). */
    Optional<Sesion> buscarEnEvento(UUID sesionId, UUID eventoId);

    void eliminar(UUID sesionId);
}
