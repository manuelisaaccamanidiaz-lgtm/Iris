package com.iris.domain.consulta;

import java.util.List;
import java.util.UUID;

/** Puerto de salida para el historial de consultas. */
public interface ConsultaRepositorio {

    /** Guarda la consulta y sus fuentes (en una transacción) y devuelve la consulta con su id. */
    Consulta guardar(Consulta consulta);

    /** Historial de UN usuario en UN evento, lo más reciente primero. */
    List<Consulta> listarDelUsuario(UUID eventoId, UUID usuarioId, int limite);
}
