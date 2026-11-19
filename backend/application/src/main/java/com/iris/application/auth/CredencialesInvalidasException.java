package com.iris.application.auth;

/**
 * Login fallido. El mensaje es SIEMPRE el mismo, sea que el email no exista o que
 * la contraseña esté mal: así un atacante no puede averiguar qué emails están registrados.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
