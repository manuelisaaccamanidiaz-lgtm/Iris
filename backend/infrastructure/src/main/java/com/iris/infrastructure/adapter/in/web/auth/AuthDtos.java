package com.iris.infrastructure.adapter.in.web.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/**
 * DTOs: la "forma" del JSON que entra y sale por HTTP. Son distintos del dominio a propósito:
 * por ejemplo, RespuestaUsuario NUNCA incluye el passwordHash.
 */
final class AuthDtos {

    private AuthDtos() {
    }

    /** Fíjate: no hay campo "rol". Si alguien lo envía en el JSON, simplemente se ignora. */
    record SolicitudRegistro(@NotBlank String nombre, @NotBlank String email, @NotBlank String password) {
    }

    record SolicitudLogin(@NotBlank String email, @NotBlank String password) {
    }

    record RespuestaUsuario(UUID id, String nombre, String email, String rol) {
    }

    record RespuestaToken(String token, String tipo, Instant expiraEn) {
    }
}
