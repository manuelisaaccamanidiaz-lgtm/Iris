package com.iris.application.port.out;

import java.time.Instant;

import com.iris.domain.usuario.Usuario;

/** Puerto de salida para emitir tokens de sesión (en infrastructure será un JWT). */
public interface GeneradorToken {

    TokenEmitido generar(Usuario usuario);

    /** El token y cuándo vence, para que el frontend sepa cuándo pedir login otra vez. */
    record TokenEmitido(String valor, Instant expiraEn) {
    }
}
