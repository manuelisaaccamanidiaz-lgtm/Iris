package com.iris.infrastructure.adapter.out.persistence.evento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tabla eventos. creadoPor es un UUID simple (no @ManyToOne a UsuarioEntity):
 * referenciamos otro agregado por su identidad. Los permisos viven en evento_miembros.
 */
@Entity
@Table(name = "eventos")
public class EventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "creado_por", nullable = false, updatable = false)
    private UUID creadoPor;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "text") // en la migración es TEXT, no VARCHAR
    private String descripcion;

    @Column(length = 200)
    private String lugar;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected EventoEntity() {
    }

    EventoEntity(UUID id, UUID creadoPor, String nombre, String descripcion, String lugar,
                 LocalDate fechaInicio, LocalDate fechaFin, Instant creadoEn) {
        this.id = id;
        this.creadoPor = creadoPor;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.creadoEn = creadoEn;
    }

    UUID getId() { return id; }
    UUID getCreadoPor() { return creadoPor; }
    String getNombre() { return nombre; }
    String getDescripcion() { return descripcion; }
    String getLugar() { return lugar; }
    LocalDate getFechaInicio() { return fechaInicio; }
    LocalDate getFechaFin() { return fechaFin; }
    Instant getCreadoEn() { return creadoEn; }
}
