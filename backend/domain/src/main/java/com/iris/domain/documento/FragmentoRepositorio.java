package com.iris.domain.documento;

import java.util.List;
import java.util.UUID;

/** Puerto de salida para fragmentos (la implementación usa SQL directo por el tipo vector). */
public interface FragmentoRepositorio {

    /**
     * Borra los fragmentos que el documento tuviera y guarda los nuevos, TODO EN UNA TRANSACCIÓN.
     * Así el procesamiento es repetible: si se reintenta, no se duplican fragmentos.
     */
    void reemplazarDelDocumento(UUID documentoId, List<Fragmento> fragmentos);

    int contarPorDocumento(UUID documentoId);

    /** El texto del documento en orden (sin los vectores): lo que Iris lee para hacer propuestas (4.4). */
    List<TextoFragmento> listarTextosDelDocumento(UUID documentoId);
}
