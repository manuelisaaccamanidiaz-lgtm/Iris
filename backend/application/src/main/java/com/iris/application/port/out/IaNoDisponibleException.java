package com.iris.application.port.out;

/**
 * El servicio de IA no respondió (límite de uso, caída, tiempo agotado...). No es un bug nuestro
 * ni un error de la persona: es "inténtalo en un rato". El adaptador traduce SU error técnico
 * (de Groq, de Spring AI) a esta excepción nuestra, y la web la convierte en un 503.
 * Así la capa de aplicación nunca depende de las excepciones de una librería concreta.
 */
public class IaNoDisponibleException extends RuntimeException {

    public IaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
