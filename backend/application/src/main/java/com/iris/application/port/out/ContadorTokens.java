package com.iris.application.port.out;

/** Puerto de salida: cuántos tokens tiene un texto (hoy: JTokkit vía Spring AI). */
public interface ContadorTokens {

    int contar(String texto);
}
