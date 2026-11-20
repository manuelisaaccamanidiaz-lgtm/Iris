package com.iris.application.auth;

import com.iris.application.port.out.CodificadorPassword;
import com.iris.domain.usuario.Email;
import com.iris.domain.usuario.Usuario;
import com.iris.domain.usuario.UsuarioRepositorio;

/**
 * Caso de uso: crear el primer administrador al arrancar la app.
 * Es idempotente: si ya existe una cuenta con ese email, no hace nada
 * (así puedes reiniciar la app mil veces sin duplicar ni sobrescribir).
 */
public class CrearAdministradorInicial {

    private final UsuarioRepositorio usuarios;
    private final CodificadorPassword codificador;

    public CrearAdministradorInicial(UsuarioRepositorio usuarios, CodificadorPassword codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    /** @return true si lo creó, false si ya existía */
    public boolean ejecutar(String nombre, String emailEscrito, String password) {
        Email email = new Email(emailEscrito);
        if (usuarios.existePorEmail(email)) {
            return false;
        }
        PoliticaPassword.validar(password);
        usuarios.guardar(Usuario.crearAdministrador(nombre, email, codificador.codificar(password)));
        return true;
    }
}
