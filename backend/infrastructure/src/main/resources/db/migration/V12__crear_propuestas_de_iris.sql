-- Día 4.4: propuestas de Iris. Iris lee un documento y PROPONE tareas o sesiones;
-- una persona (EDITOR o PROPIETARIO) las acepta, con o sin cambios, o las descarta.

CREATE TABLE propuestas (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_id         UUID          NOT NULL,
    -- De qué documento salió. SET NULL: si borran el documento (4.5), la propuesta aceptada
    -- sigue existiendo como historial...
    documento_id      UUID,
    -- ...y por eso copiamos el nombre: la cita sigue siendo legible aunque el documento ya no esté
    documento_nombre  VARCHAR(255)  NOT NULL,
    tipo              VARCHAR(10)   NOT NULL,
    estado            VARCHAR(15)   NOT NULL DEFAULT 'PENDIENTE',
    titulo            VARCHAR(200)  NOT NULL,
    descripcion       TEXT,
    -- TAREA: fecha límite (opcional). SESION: fecha y horas (obligatorias, ver CHECK)
    fecha             DATE,
    hora_inicio       TIME,
    hora_fin          TIME,
    sala              VARCHAR(100),
    tipo_sesion       VARCHAR(20),
    -- "Iris siempre cita sus fuentes": la frase TEXTUAL del documento que justifica la propuesta
    cita              VARCHAR(500)  NOT NULL,
    pagina_inicio     INT           NOT NULL,
    pagina_fin        INT           NOT NULL,
    creada_en         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    decidida_en       TIMESTAMPTZ,
    decidida_por      UUID,
    -- El id de la tarea o sesión que se creó al aceptar. Sin FK a propósito: puede apuntar a
    -- DOS tablas distintas (tareas o sesiones) según el tipo, y una FK solo apunta a una
    resultado_id      UUID,

    CONSTRAINT fk_propuestas_evento FOREIGN KEY (evento_id)
        REFERENCES eventos (id) ON DELETE CASCADE,
    CONSTRAINT fk_propuestas_documento FOREIGN KEY (documento_id)
        REFERENCES documentos (id) ON DELETE SET NULL,
    CONSTRAINT fk_propuestas_decidida_por FOREIGN KEY (decidida_por)
        REFERENCES usuarios (id) ON DELETE SET NULL,
    CONSTRAINT ck_propuestas_tipo CHECK (tipo IN ('TAREA', 'SESION')),
    CONSTRAINT ck_propuestas_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'DESCARTADA')),
    CONSTRAINT ck_propuestas_tipo_sesion
        CHECK (tipo_sesion IS NULL OR tipo_sesion IN ('CONFERENCIA', 'TALLER', 'PANEL', 'REUNION', 'LOGISTICA', 'OTRO')),
    -- Una sesión necesita fecha y horas coherentes (las mismas reglas que la tabla sesiones)
    CONSTRAINT ck_propuestas_sesion CHECK (
        tipo <> 'SESION' OR (fecha IS NOT NULL AND hora_inicio IS NOT NULL AND hora_fin IS NOT NULL
                             AND hora_fin > hora_inicio)),
    CONSTRAINT ck_propuestas_paginas CHECK (pagina_inicio >= 1 AND pagina_fin >= pagina_inicio),
    -- El truco de siempre: PENDIENTE <=> sin decisión; ACEPTADA <=> hay un resultado
    CONSTRAINT ck_propuestas_decidida CHECK ((estado = 'PENDIENTE') = (decidida_en IS NULL)),
    CONSTRAINT ck_propuestas_resultado CHECK ((estado = 'ACEPTADA') = (resultado_id IS NOT NULL))
);

-- "Las propuestas pendientes del evento, las más nuevas primero"
CREATE INDEX ix_propuestas_evento_estado ON propuestas (evento_id, estado, creada_en DESC);

-- Para reemplazar las pendientes de un documento cuando se vuelve a revisar (decisión de Isaac)
CREATE INDEX ix_propuestas_documento_pendientes ON propuestas (documento_id) WHERE estado = 'PENDIENTE';

-- Un tipo de notificación nuevo: "Iris leyó tu documento y te propone N cosas".
-- Un CHECK no se edita: se borra y se vuelve a crear con la lista nueva
ALTER TABLE notificaciones DROP CONSTRAINT ck_notificaciones_tipo;
ALTER TABLE notificaciones ADD CONSTRAINT ck_notificaciones_tipo CHECK (tipo IN (
    'INVITACION_RECIBIDA', 'MIEMBRO_NUEVO', 'SESION_NUEVA', 'TAREA_NUEVA', 'TAREA_ASIGNADA',
    'DOCUMENTO_INDEXADO', 'TAREA_VENCE_MANANA', 'TAREA_VENCE_HOY', 'TAREA_VENCIDA', 'PROPUESTAS_NUEVAS'));
