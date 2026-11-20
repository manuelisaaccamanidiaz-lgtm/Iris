package com.iris.application.auth;

import com.iris.application.port.out.CodificadorPassword;
import com.iris.application.port.out.GeneradorToken;
import com.iris.application.port.out.GeneradorToken.TokenEmitido;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.usuario.Email;
import com.iris.domain.usuario.Usuario;
import com.iris.domain.usuario.UsuarioRepositorio;

/** Caso de uso: verificar credenciales y emitir un token. */
public class IniciarSesion {

    private final UsuarioRepositorio usuarios;
    private final CodificadorPassword codificador;
    private final GeneradorToken generadorToken;

    public IniciarSesion(UsuarioRepositorio usuarios, CodificadorPassword codificador,
                         GeneradorToken generadorToken) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.generadorToken = generadorToken;
    }

    public TokenEmitido ejecutar(String emailEscrito, String password) {
        Email email;
        try {
            email = new Email(emailEscrito);
        } catch (ReglaDeNegocioException e) {
            throw new CredencialesInvalidasException(); // no revelamos el motivo
        }

        Usuario usuario = usuarios.buscarPorEmail(email)
                .orElseThrow(CredencialesInvalidasException::new);

        boolean passwordCorrecta = password != null && codificador.coincide(password, usuario.passwordHash());
        if (!passwordCorrecta || !usuario.activo()) {
            throw new CredencialesInvalidasException();
        }

        return generadorToken.generar(usuario);
    }
}
