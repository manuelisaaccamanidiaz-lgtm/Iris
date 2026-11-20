package com.iris.infrastructure.adapter.out.persistence.evento;

import java.time.Instant;

import com.iris.domain.evento.RolEnEvento;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Tabla evento_miembros: quién pertenece a qué evento y con qué rol. */
@Entity
@Table(name = "evento_miembros")
public class MiembroEventoEntity {

    @EmbeddedId // la PK es el objeto MiembroEventoId (dos columnas)
    private MiembroEventoId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolEnEvento rol;

    @Column(name = "unido_en", nullable = false, updatable = false)
    private Instant unidoEn;

    protected MiembroEventoEntity() {
    }

    MiembroEventoEntity(MiembroEventoId id, RolEnEvento rol, Instant unidoEn) {
        this.id = id;
        this.rol = rol;
        this.unidoEn = unidoEn;
    }

    MiembroEventoId getId() { return id; }
    RolEnEvento getRol() { return rol; }
    Instant getUnidoEn() { return unidoEn; }
}
