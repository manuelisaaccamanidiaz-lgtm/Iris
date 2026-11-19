package com.iris.application.auth;

import com.iris.application.port.out.CodificadorPassword;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.usuario.Email;
import com.iris.domain.usuario.EmailYaRegistradoException;
import com.iris.domain.usuario.Usuario;
import com.iris.domain.usuario.UsuarioRepositorio;

/**
 * Caso de uso: registro público de un organizador.
 * Clase de Java puro: sin @Service. Infrastructure la crea como bean (lo veremos en el paso 5b).
 */
public class RegistrarUsuario {

    // BCrypt solo usa los primeros 72 bytes: más largo sería una falsa sensación de seguridad
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 72;

    private final UsuarioRepositorio usuarios;
    private final CodificadorPassword codificador;

    public RegistrarUsuario(UsuarioRepositorio usuarios, CodificadorPassword codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    public Usuario ejecutar(Comando comando) {
        Email email = new Email(comando.email()); // valida el formato
        validarPassword(comando.password());

        if (usuarios.existePorEmail(email)) {
            throw new EmailYaRegistradoException();
        }

        String hash = codificador.codificar(comando.password());
        // registrar() siempre asigna ORGANIZADOR: el comando ni siquiera tiene campo "rol"
        return usuarios.guardar(Usuario.registrar(comando.nombre(), email, hash));
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN) {
            throw new ReglaDeNegocioException("La contraseña debe tener al menos " + PASSWORD_MIN + " caracteres");
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > PASSWORD_MAX) {
            throw new ReglaDeNegocioException("La contraseña es demasiado larga (máximo " + PASSWORD_MAX + " bytes)");
        }
    }

    /** Datos de entrada del caso de uso (un "comando"). */
    public record Comando(String nombre, String email, String password) {
    }
}
