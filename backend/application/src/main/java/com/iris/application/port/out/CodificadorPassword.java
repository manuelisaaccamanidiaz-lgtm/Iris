package com.iris.application.port.out;

/**
 * Puerto de salida para convertir contraseñas en hash y verificarlas.
 * La capa application no sabe que detrás está BCrypt (eso es de infrastructure).
 */
public interface CodificadorPassword {

    String codificar(String passwordPlano);

    boolean coincide(String passwordPlano, String hashGuardado);
}
