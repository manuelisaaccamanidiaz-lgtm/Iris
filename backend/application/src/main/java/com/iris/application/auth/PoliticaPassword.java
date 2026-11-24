package com.iris.application.auth;

import java.nio.charset.StandardCharsets;

import com.iris.domain.compartido.ReglaDeNegocioException;

/** Reglas de contraseña en un solo lugar (las usan el registro y el admin inicial). */
final class PoliticaPassword {

    // BCrypt solo usa los primeros 72 bytes: más largo sería una falsa sensación de seguridad
    static final int MINIMO = 8;
    static final int MAXIMO_BYTES = 72;

    private PoliticaPassword() {
    }

    static void validar(String password) {
        if (password == null || password.length() < MINIMO) {
            throw new ReglaDeNegocioException("La contraseña debe tener al menos " + MINIMO + " caracteres");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            throw new ReglaDeNegocioException("La contraseña es demasiado larga (máximo " + MAXIMO_BYTES + " bytes)");
        }
    }
}
