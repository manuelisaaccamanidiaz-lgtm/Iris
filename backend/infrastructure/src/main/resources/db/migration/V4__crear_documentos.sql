-- PDFs subidos a un evento. OJO: aquí NO se guarda el PDF, solo sus metadatos.
-- El archivo original se queda en el computador del usuario (requisito del profe).

-- Truco multi-tenant: permite que otras tablas referencien (evento_id, usuario_id) JUNTOS.
-- Así la BD garantiza que el usuario_id de un documento sea el dueño REAL del evento.
ALTER TABLE eventos ADD CONSTRAINT uq_eventos_id_usuario UNIQUE (id, usuario_id);

CREATE TABLE documentos (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_id         UUID         NOT NULL,
    usuario_id        UUID         NOT NULL,
    nombre_archivo    VARCHAR(255) NOT NULL,
    tamano_bytes      BIGINT       NOT NULL,
    -- Huella SHA-256 del archivo: detecta si suben el mismo PDF dos veces
    hash_sha256       VARCHAR(64)  NOT NULL,
    paginas           INTEGER,
    estado            VARCHAR(20)  NOT NULL DEFAULT 'EN_COLA',
    error_mensaje     TEXT,
    -- Trazabilidad: con qué modelo y tamaño se generaron los vectores
    modelo_embedding  VARCHAR(100),
    dimensiones       INTEGER,
    -- Copia temporal mientras se indexa; queda NULL cuando se borra (paso 7 del flujo)
    ruta_temporal     VARCHAR(500),
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    indexado_en       TIMESTAMPTZ,

    -- FK compuesta: el documento apunta al evento Y a su dueño a la vez.
    -- Si alguien intenta guardar un documento de Ana en un evento de Beto, la BD lo rechaza.
    CONSTRAINT fk_documentos_evento FOREIGN KEY (evento_id, usuario_id)
        REFERENCES eventos (id, usuario_id) ON DELETE CASCADE,
    CONSTRAINT ck_documentos_estado CHECK (estado IN ('EN_COLA', 'PROCESANDO', 'INDEXADO', 'ERROR')),
    CONSTRAINT ck_documentos_tamano CHECK (tamano_bytes > 0),
    -- El mismo PDF no se puede subir dos veces al mismo evento
    CONSTRAINT uq_documentos_evento_hash UNIQUE (evento_id, hash_sha256),
    -- Permite que fragmentos referencie (id, evento_id, usuario_id) juntos (mismo truco)
    CONSTRAINT uq_documentos_id_evento_usuario UNIQUE (id, evento_id, usuario_id)
);

-- "Los documentos de este evento de este usuario" (la consulta del panel derecho)
CREATE INDEX ix_documentos_usuario_evento ON documentos (usuario_id, evento_id);

-- Índice PARCIAL: solo indexa los pendientes. Al arrancar la app buscaremos
-- los que quedaron EN_COLA o PROCESANDO para re-encolarlos, y esta búsqueda será instantánea.
CREATE INDEX ix_documentos_pendientes ON documentos (estado)
    WHERE estado IN ('EN_COLA', 'PROCESANDO');
