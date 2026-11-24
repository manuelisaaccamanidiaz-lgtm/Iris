package com.iris.domain.usuario;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: lo que el dominio NECESITA para guardar usuarios,
 * sin saber si detrás hay PostgreSQL, un archivo o un mock de pruebas.
 * La implementación vive en infrastructure (adaptador JPA).
 */
public interface UsuarioRepositorio {

    /** Guarda y devuelve el usuario ya con su id asignado. */
    Usuario guardar(Usuario usuario);

    /** Busca ignorando mayúsculas. */
    Optional<Usuario> buscarPorEmail(Email email);

    boolean existePorEmail(Email email);

    /** Para saber el email de quien hace la petición (el JWT solo trae su id). */
    Optional<Usuario> buscarPorId(UUID id);
}
