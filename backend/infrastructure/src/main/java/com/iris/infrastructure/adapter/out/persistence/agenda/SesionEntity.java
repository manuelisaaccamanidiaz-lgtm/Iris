package com.iris.infrastructure.adapter.out.persistence.agenda;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.iris.domain.agenda.OrigenActividad;
import com.iris.domain.agenda.TipoSesion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla sesiones. LocalTime <-> TIME y LocalDate <-> DATE los convierte Hibernate solo. */
@Entity
@Table(name = "sesiones")
public class SesionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evento_id", nullable = false, updatable = false)
    private UUID eventoId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(length = 100)
    private String sala;

    @Column(length = 150)
    private String ponente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSesion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private OrigenActividad origen;

    @Column(name = "creado_por", updatable = false)
    private UUID creadoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected SesionEntity() {
    }

    SesionEntity(UUID id, UUID eventoId, String titulo, String descripcion, LocalDate fecha, LocalTime horaInicio,
                 LocalTime horaFin, String sala, String ponente, TipoSesion tipo, OrigenActividad origen,
                 UUID creadoPor, Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.eventoId = eventoId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.sala = sala;
        this.ponente = ponente;
        this.tipo = tipo;
        this.origen = origen;
        this.creadoPor = creadoPor;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    UUID getId() { return id; }
    UUID getEventoId() { return eventoId; }
    String getTitulo() { return titulo; }
    String getDescripcion() { return descripcion; }
    LocalDate getFecha() { return fecha; }
    LocalTime getHoraInicio() { return horaInicio; }
    LocalTime getHoraFin() { return horaFin; }
    String getSala() { return sala; }
    String getPonente() { return ponente; }
    TipoSesion getTipo() { return tipo; }
    OrigenActividad getOrigen() { return origen; }
    UUID getCreadoPor() { return creadoPor; }
    Instant getCreadoEn() { return creadoEn; }
    Instant getActualizadoEn() { return actualizadoEn; }
}
