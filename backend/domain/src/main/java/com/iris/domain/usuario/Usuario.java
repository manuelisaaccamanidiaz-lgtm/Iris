package com.iris.domain.usuario;

import java.time.Instant;
import java.util.UUID;

import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Un organizador (o administrador) de Iris. Inmutable: para "cambiarlo" se crea otro.
 *
 * @param id           null mientras no se ha guardado; la base de datos lo asigna
 * @param passwordHash nunca la contraseña real, solo su hash
 */
public record Usuario(
        UUID id,
        String nombre,
        Email email,
        String passwordHash,
        Rol rol,
        boolean activo,
        Instant creadoEn) {

    public Usuario {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre es obligatorio");
        }
        nombre = nombre.trim();
        if (nombre.length() > 120) {
            throw new ReglaDeNegocioException("El nombre no puede superar 120 caracteres");
        }
        if (email == null || passwordHash == null || rol == null || creadoEn == null) {
            throw new ReglaDeNegocioException("Faltan datos obligatorios del usuario");
        }
    }

    /**
     * Única puerta para crear cuentas nuevas desde el registro público.
     * El rol NO es un parámetro: siempre ORGANIZADOR. Así nadie puede auto-ascenderse a ADMIN.
     */
    public static Usuario registrar(String nombre, Email email, String passwordHash) {
        return new Usuario(null, nombre, email, passwordHash, Rol.ORGANIZADOR, true, Instant.now());
    }

    /**
     * Puerta SEPARADA y explícita para crear administradores. Solo la usa el arranque
     * de la app (con datos del .env); ningún endpoint público llega aquí.
     */
    public static Usuario crearAdministrador(String nombre, Email email, String passwordHash) {
        return new Usuario(null, nombre, email, passwordHash, Rol.ADMIN, true, Instant.now());
    }

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }
}
