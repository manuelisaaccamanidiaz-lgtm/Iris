package com.iris.domain.usuario;

import java.util.Locale;
import java.util.regex.Pattern;

import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Objeto de valor: un email que, si existe, ya es válido.
 *
 * <p>Guarda el email TAL COMO lo escribió el usuario (el estándar SMTP pide
 * conservar las mayúsculas al enviar correos), pero para comparar usa
 * {@link #normalizado()}: así Ana@iris.com y ana@iris.com son la misma cuenta.
 */
public record Email(String valor) {

    // Validación sencilla a propósito: algo@algo.algo, sin espacios
    private static final Pattern FORMATO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    // Constructor compacto del record: se ejecuta SIEMPRE al crear un Email
    public Email {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDeNegocioException("El email es obligatorio");
        }
        valor = valor.trim();
        if (valor.length() > 255 || !FORMATO.matcher(valor).matches()) {
            throw new ReglaDeNegocioException("El email no tiene un formato válido");
        }
    }

    /** Versión en minúsculas, solo para comparar y buscar. */
    public String normalizado() {
        return valor.toLowerCase(Locale.ROOT);
    }
}
