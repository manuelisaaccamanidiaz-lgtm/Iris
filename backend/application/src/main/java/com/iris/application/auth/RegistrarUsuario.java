package com.iris.application.auth;

import com.iris.application.port.out.CodificadorPassword;
import com.iris.domain.usuario.Email;
import com.iris.domain.usuario.EmailYaRegistradoException;
import com.iris.domain.usuario.Usuario;
import com.iris.domain.usuario.UsuarioRepositorio;

/**
 * Caso de uso: registro público de un organizador.
 * Clase de Java puro: sin @Service. Infrastructure la crea como bean en CasosDeUsoConfig.
 */
public class RegistrarUsuario {

    private final UsuarioRepositorio usuarios;
    private final CodificadorPassword codificador;

    public RegistrarUsuario(UsuarioRepositorio usuarios, CodificadorPassword codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    public Usuario ejecutar(Comando comando) {
        Email email = new Email(comando.email()); // valida el formato
        PoliticaPassword.validar(comando.password());

        if (usuarios.existePorEmail(email)) {
            throw new EmailYaRegistradoException();
        }

        String hash = codificador.codificar(comando.password());
        // registrar() siempre asigna ORGANIZADOR: el comando ni siquiera tiene campo "rol"
        return usuarios.guardar(Usuario.registrar(comando.nombre(), email, hash));
    }

    /** Datos de entrada del caso de uso (un "comando"). */
    public record Comando(String nombre, String email, String password) {
    }
}
