package com.iris.infrastructure.adapter.out.persistence.documento;

import java.time.Instant;
import java.util.UUID;

import com.iris.domain.documento.EstadoDocumento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla documentos (solo metadatos; el PDF nunca se guarda en la BD). */
@Entity
@Table(name = "documentos")
public class DocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evento_id", nullable = false, updatable = false)
    private UUID eventoId;

    @Column(name = "subido_por")
    private UUID subidoPor;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    private Integer paginas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado;

    @Column(name = "error_mensaje", columnDefinition = "text")
    private String errorMensaje;

    @Column(name = "modelo_embedding", length = 100)
    private String modeloEmbedding;

    private Integer dimensiones;

    @Column(name = "ruta_temporal", length = 500)
    private String rutaTemporal;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Column(name = "indexado_en")
    private Instant indexadoEn;

    protected DocumentoEntity() {
    }

    DocumentoEntity(UUID id, UUID eventoId, UUID subidoPor, String nombreArchivo, long tamanoBytes,
                    String hashSha256, Integer paginas, EstadoDocumento estado, String errorMensaje,
                    String modeloEmbedding, Integer dimensiones, String rutaTemporal,
                    Instant creadoEn, Instant actualizadoEn, Instant indexadoEn) {
        this.id = id;
        this.eventoId = eventoId;
        this.subidoPor = subidoPor;
        this.nombreArchivo = nombreArchivo;
        this.tamanoBytes = tamanoBytes;
        this.hashSha256 = hashSha256;
        this.paginas = paginas;
        this.estado = estado;
        this.errorMensaje = errorMensaje;
        this.modeloEmbedding = modeloEmbedding;
        this.dimensiones = dimensiones;
        this.rutaTemporal = rutaTemporal;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
        this.indexadoEn = indexadoEn;
    }

    UUID getId() { return id; }
    UUID getEventoId() { return eventoId; }
    UUID getSubidoPor() { return subidoPor; }
    String getNombreArchivo() { return nombreArchivo; }
    long getTamanoBytes() { return tamanoBytes; }
    String getHashSha256() { return hashSha256; }
    Integer getPaginas() { return paginas; }
    EstadoDocumento getEstado() { return estado; }
    String getErrorMensaje() { return errorMensaje; }
    String getModeloEmbedding() { return modeloEmbedding; }
    Integer getDimensiones() { return dimensiones; }
    String getRutaTemporal() { return rutaTemporal; }
    Instant getCreadoEn() { return creadoEn; }
    Instant getActualizadoEn() { return actualizadoEn; }
    Instant getIndexadoEn() { return indexadoEn; }
}
