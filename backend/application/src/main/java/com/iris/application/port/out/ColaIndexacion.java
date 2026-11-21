package com.iris.application.port.out;

import java.util.UUID;

/**
 * Puerto de salida: "pon este documento en la fila para indexarlo".
 * El caso de uso no sabe CÓMO se procesa en segundo plano (hoy @Async; mañana podría ser otra cola).
 * Contrato: encolar() regresa de inmediato, sin esperar a que termine el procesamiento.
 */
public interface ColaIndexacion {

    void encolar(UUID documentoId);
}
