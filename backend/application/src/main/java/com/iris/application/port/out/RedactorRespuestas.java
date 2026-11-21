package com.iris.application.port.out;

import java.util.function.Consumer;

/**
 * Puerto de salida: un modelo de lenguaje que REDACTA una respuesta a partir de unas reglas
 * (instrucciones de sistema) y un mensaje (contexto + pregunta). Hoy lo implementa Groq.
 * Fíjate: application no sabe nada de Groq, de HTTP ni de Spring AI.
 */
public interface RedactorRespuestas {

    /** Respuesta completa de una vez. */
    String redactar(String instrucciones, String mensaje);

    /**
     * Respuesta "en vivo": va entregando pedacitos de texto a medida que el modelo los genera
     * (para el streaming del chat). Es Java puro: un Consumer que recibe cada trozo.
     * Bloquea hasta que el modelo termina.
     */
    void redactarEnVivo(String instrucciones, String mensaje, Consumer<String> alRecibirTrozo);

    /** Nombre del modelo, para la trazabilidad. */
    String modelo();
}
