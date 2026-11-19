-- Eventos que organiza cada usuario. Primera tabla "multi-tenant":
-- cada fila tiene dueño (usuario_id) y TODA consulta filtrará por él.
CREATE TABLE eventos (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id    UUID         NOT NULL,
    nombre        VARCHAR(150) NOT NULL,
    descripcion   TEXT,
    lugar         VARCHAR(200),
    fecha_inicio  DATE         NOT NULL,
    fecha_fin     DATE         NOT NULL,
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Si se borra el usuario, se borran sus eventos (y luego, en cascada, todo lo demás)
    CONSTRAINT fk_eventos_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_eventos_fechas CHECK (fecha_fin >= fecha_inicio)
);

-- Índice para el filtro que haremos SIEMPRE: "los eventos de este usuario"
CREATE INDEX ix_eventos_usuario ON eventos (usuario_id);
