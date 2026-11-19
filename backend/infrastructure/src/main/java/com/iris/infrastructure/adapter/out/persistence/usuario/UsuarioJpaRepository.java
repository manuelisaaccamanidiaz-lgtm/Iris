package com.iris.infrastructure.adapter.out.persistence.usuario;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de Spring Data: Spring genera la implementación en tiempo de ejecución. */
interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    // lower(...) en ambos lados: coincide con el índice ux_usuarios_email de la migración V2
    @Query("select u from UsuarioEntity u where lower(u.email) = :emailNormalizado")
    Optional<UsuarioEntity> buscarPorEmailNormalizado(@Param("emailNormalizado") String emailNormalizado);

    @Query("select count(u) > 0 from UsuarioEntity u where lower(u.email) = :emailNormalizado")
    boolean existePorEmailNormalizado(@Param("emailNormalizado") String emailNormalizado);
}
