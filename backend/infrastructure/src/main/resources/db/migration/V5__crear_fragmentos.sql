-- Fragmentos de texto de cada documento, con su embedding. El corazón del RAG.
CREATE TABLE fragmentos (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id    UUID          NOT NULL,
    -- Copiados del documento A PROPÓSITO (desnormalización): la búsqueda vectorial
    -- filtra por dueño y evento SIN hacer JOIN, que es más rápido y más difícil de olvidar
    evento_id       UUID          NOT NULL,
    usuario_id      UUID          NOT NULL,
    -- Posición del fragmento dentro del documento (0, 1, 2...)
    orden           INTEGER       NOT NULL,
    -- Un fragmento puede cruzar páginas (decisión 3A): cita "págs. 4–5"
    pagina_inicio   INTEGER       NOT NULL,
    pagina_fin      INTEGER       NOT NULL,
    seccion         VARCHAR(300),
    contenido       TEXT          NOT NULL,
    tokens          INTEGER       NOT NULL,
    -- 1536 dimensiones (Gemini recortado con MRL). Debe ser el MISMO al indexar y al consultar
    embedding       vector(1536)  NOT NULL,
    -- Columna GENERADA: PostgreSQL calcula sola la versión "buscable por palabras" del texto,
    -- en español (quita tildes de raíces, palabras vacías como "el", "de"...). Se usa en el Día 3
    contenido_tsv   tsvector      GENERATED ALWAYS AS (to_tsvector('spanish', contenido)) STORED,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- FK compuesta: el fragmento hereda dueño y evento EXACTOS de su documento
    CONSTRAINT fk_fragmentos_documento FOREIGN KEY (documento_id, evento_id, usuario_id)
        REFERENCES documentos (id, evento_id, usuario_id) ON DELETE CASCADE,
    CONSTRAINT ck_fragmentos_paginas CHECK (pagina_inicio >= 1 AND pagina_fin >= pagina_inicio),
    CONSTRAINT ck_fragmentos_tokens CHECK (tokens > 0),
    CONSTRAINT uq_fragmentos_documento_orden UNIQUE (documento_id, orden)
);

-- Filtro multi-tenant de TODA búsqueda: "fragmentos de este usuario en este evento"
CREATE INDEX ix_fragmentos_usuario_evento ON fragmentos (usuario_id, evento_id);

-- Índice vectorial HNSW con distancia coseno (operador <=>): búsqueda por significado
CREATE INDEX ix_fragmentos_embedding ON fragmentos USING hnsw (embedding vector_cosine_ops);

-- Índice GIN sobre el tsvector: búsqueda por palabras exactas ("penalidad", "30 días")
CREATE INDEX ix_fragmentos_tsv ON fragmentos USING gin (contenido_tsv);
