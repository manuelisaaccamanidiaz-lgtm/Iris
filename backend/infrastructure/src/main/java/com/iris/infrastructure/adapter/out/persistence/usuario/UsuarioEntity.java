package com.iris.infrastructure.adapter.out.persistence.usuario;

import java.time.Instant;
import java.util.UUID;

import com.iris.domain.usuario.Rol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representación de la tabla usuarios para JPA. Es un detalle de infraestructura:
 * el resto de la app trabaja con el record Usuario del dominio, no con esta clase.
 */
@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Hibernate genera el UUID al insertar
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING) // guarda "ORGANIZADOR", no 0 o 1
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** JPA exige un constructor vacío; protected para que nadie más lo use. */
    protected UsuarioEntity() {
    }

    UsuarioEntity(UUID id, String nombre, String email, String passwordHash,
                  Rol rol, boolean activo, Instant creadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
        this.creadoEn = creadoEn;
    }

    UUID getId() { return id; }
    String getNombre() { return nombre; }
    String getEmail() { return email; }
    String getPasswordHash() { return passwordHash; }
    Rol getRol() { return rol; }
    boolean isActivo() { return activo; }
    Instant getCreadoEn() { return creadoEn; }
}
