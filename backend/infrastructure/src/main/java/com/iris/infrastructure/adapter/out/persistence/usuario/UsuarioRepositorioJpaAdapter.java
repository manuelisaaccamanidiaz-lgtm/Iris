package com.iris.infrastructure.adapter.out.persistence.usuario;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.iris.domain.usuario.Email;
import com.iris.domain.usuario.Usuario;
import com.iris.domain.usuario.UsuarioRepositorio;

/**
 * Adaptador de salida: "enchufa" el puerto UsuarioRepositorio del dominio a PostgreSQL vía JPA.
 * Traduce Usuario (dominio) <-> UsuarioEntity (tabla) en ambas direcciones.
 */
@Component
class UsuarioRepositorioJpaAdapter implements UsuarioRepositorio {

    private final UsuarioJpaRepository jpa;

    UsuarioRepositorioJpaAdapter(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity guardado = jpa.save(aEntity(usuario));
        return aDominio(guardado);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(Email email) {
        return jpa.buscarPorEmailNormalizado(email.normalizado()).map(this::aDominio);
    }

    @Override
    public boolean existePorEmail(Email email) {
        return jpa.existePorEmailNormalizado(email.normalizado());
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::aDominio);
    }

    private UsuarioEntity aEntity(Usuario u) {
        return new UsuarioEntity(u.id(), u.nombre(), u.email().valor(), u.passwordHash(),
                u.rol(), u.activo(), u.creadoEn());
    }

    private Usuario aDominio(UsuarioEntity e) {
        return new Usuario(e.getId(), e.getNombre(), new Email(e.getEmail()), e.getPasswordHash(),
                e.getRol(), e.isActivo(), e.getCreadoEn());
    }
}
