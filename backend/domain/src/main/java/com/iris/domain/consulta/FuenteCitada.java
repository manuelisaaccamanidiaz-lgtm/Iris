package com.iris.domain.consulta;

import java.util.UUID;

/**
 * Una fuente que se le entregó al modelo con el número [n], y si de verdad la citó.
 * Así se arma la lista "[1] Contrato_Hotel_Cumbre_Andina.pdf · pág. 2" que ve el usuario.
 */
public record FuenteCitada(int numero, UUID fragmentoId, UUID documentoId, String documento,
                           String paginas, String seccion, boolean citada) {

    public FuenteCitada marcarCitada(boolean valor) {
        return new FuenteCitada(numero, fragmentoId, documentoId, documento, paginas, seccion, valor);
    }
}
