-- Colaboración: un evento ya no tiene UN dueño, tiene MIEMBROS con un rol.
-- Regla de Flyway: no editamos V3-V5 (ya aplicadas); este archivo los ajusta.

-- 1) Soltar las FK compuestas que asumían "un dueño por evento"
--    (orden importa: primero quien referencia, luego lo referenciado)
ALTER TABLE fragmentos DROP CONSTRAINT fk_fragmentos_documento;
ALTER TABLE documentos DROP CONSTRAINT fk_documentos_evento;
ALTER TABLE documentos DROP CONSTRAINT uq_documentos_id_evento_usuario;
ALTER TABLE eventos    DROP CONSTRAINT uq_eventos_id_usuario;

-- 2) La nueva tabla de miembros
CREATE TABLE evento_miembros (
    evento_id   UUID        NOT NULL,
    usuario_id  UUID        NOT NULL,
    rol         VARCHAR(20) NOT NULL,
    unido_en    TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- PK compuesta: una persona aparece UNA sola vez por evento
    CONSTRAINT pk_evento_miembros PRIMARY KEY (evento_id, usuario_id),
    CONSTRAINT fk_miembros_evento FOREIGN KEY (evento_id)
        REFERENCES eventos (id) ON DELETE CASCADE,
    CONSTRAINT fk_miembros_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_miembros_rol CHECK (rol IN ('PROPIETARIO', 'EDITOR', 'LECTOR'))
);

-- "¿De qué eventos soy miembro?" (la PK ya cubre la búsqueda por evento)
CREATE INDEX ix_evento_miembros_usuario ON evento_miembros (usuario_id);

-- Índice único PARCIAL: máximo UN propietario por evento
CREATE UNIQUE INDEX ux_evento_un_propietario ON evento_miembros (evento_id)
    WHERE rol = 'PROPIETARIO';

-- 3) Los eventos que ya existen: su creador pasa a ser PROPIETARIO
INSERT INTO evento_miembros (evento_id, usuario_id, rol, unido_en)
SELECT id, usuario_id, 'PROPIETARIO', creado_en FROM eventos;

-- 4) eventos.usuario_id ahora solo dice QUIÉN LO CREÓ (los permisos están en miembros)
ALTER TABLE eventos RENAME COLUMN usuario_id TO creado_por;
ALTER TABLE eventos RENAME CONSTRAINT fk_eventos_usuario TO fk_eventos_creado_por;
ALTER INDEX ix_eventos_usuario RENAME TO ix_eventos_creado_por;

-- 5) documentos: usuario_id pasa a ser "quién lo subió".
--    Si esa persona borra su cuenta, el documento SE QUEDA para el equipo (SET NULL)
ALTER TABLE documentos RENAME COLUMN usuario_id TO subido_por;
ALTER TABLE documentos ALTER COLUMN subido_por DROP NOT NULL;
ALTER TABLE documentos ADD CONSTRAINT fk_documentos_subido_por FOREIGN KEY (subido_por)
    REFERENCES usuarios (id) ON DELETE SET NULL;
ALTER TABLE documentos ADD CONSTRAINT fk_documentos_evento FOREIGN KEY (evento_id)
    REFERENCES eventos (id) ON DELETE CASCADE;
DROP INDEX ix_documentos_usuario_evento;
CREATE INDEX ix_documentos_evento ON documentos (evento_id);
-- Para que fragmentos pueda referenciar (id, evento_id) juntos
ALTER TABLE documentos ADD CONSTRAINT uq_documentos_id_evento UNIQUE (id, evento_id);

-- 6) fragmentos: ya no necesita usuario_id; se filtra por EVENTO (tras verificar membresía)
DROP INDEX ix_fragmentos_usuario_evento;
ALTER TABLE fragmentos DROP COLUMN usuario_id;
-- FK compuesta: el evento_id copiado en el fragmento SIEMPRE coincide con el de su documento
ALTER TABLE fragmentos ADD CONSTRAINT fk_fragmentos_documento FOREIGN KEY (documento_id, evento_id)
    REFERENCES documentos (id, evento_id) ON DELETE CASCADE;
CREATE INDEX ix_fragmentos_evento ON fragmentos (evento_id);
