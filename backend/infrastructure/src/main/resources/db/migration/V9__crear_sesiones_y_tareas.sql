-- Agenda (sesiones) y pendientes (tareas) de cada evento.

CREATE TABLE sesiones (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_id       UUID          NOT NULL,
    titulo          VARCHAR(200)  NOT NULL,
    descripcion     TEXT,
    fecha           DATE          NOT NULL,
    hora_inicio     TIME          NOT NULL,
    hora_fin        TIME          NOT NULL,
    sala            VARCHAR(100),
    ponente         VARCHAR(150),
    -- El tipo define el color en la interfaz ("un color por tipo de actividad")
    tipo            VARCHAR(20)   NOT NULL DEFAULT 'CONFERENCIA',
    -- MANUAL = la creó una persona; IRIS = vino de una propuesta aceptada (Día 4.4)
    origen          VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
    creado_por      UUID,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_sesiones_evento FOREIGN KEY (evento_id)
        REFERENCES eventos (id) ON DELETE CASCADE,
    CONSTRAINT fk_sesiones_creado_por FOREIGN KEY (creado_por)
        REFERENCES usuarios (id) ON DELETE SET NULL,
    CONSTRAINT ck_sesiones_horas CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_sesiones_tipo CHECK (tipo IN ('CONFERENCIA', 'TALLER', 'PANEL', 'REUNION', 'LOGISTICA', 'OTRO')),
    CONSTRAINT ck_sesiones_origen CHECK (origen IN ('MANUAL', 'IRIS'))
);

-- "La agenda del evento en orden cronológico"
CREATE INDEX ix_sesiones_evento_fecha ON sesiones (evento_id, fecha, hora_inicio);

CREATE TABLE tareas (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_id       UUID          NOT NULL,
    titulo          VARCHAR(200)  NOT NULL,
    descripcion     TEXT,
    fecha_limite    DATE,
    estado          VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    -- Responsable opcional. DEBE ser miembro del evento (FK compuesta a evento_miembros)
    responsable_id  UUID,
    origen          VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
    creado_por      UUID,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    completada_en   TIMESTAMPTZ,

    CONSTRAINT fk_tareas_evento FOREIGN KEY (evento_id)
        REFERENCES eventos (id) ON DELETE CASCADE,
    -- FK compuesta: (evento, responsable) tiene que existir en evento_miembros.
    -- "SET NULL (responsable_id)" (PostgreSQL 15+): si quitan a esa persona del evento,
    -- SOLO se borra el responsable; la tarea y su evento_id se quedan intactos
    CONSTRAINT fk_tareas_responsable FOREIGN KEY (evento_id, responsable_id)
        REFERENCES evento_miembros (evento_id, usuario_id) ON DELETE SET NULL (responsable_id),
    CONSTRAINT fk_tareas_creado_por FOREIGN KEY (creado_por)
        REFERENCES usuarios (id) ON DELETE SET NULL,
    CONSTRAINT ck_tareas_estado CHECK (estado IN ('PENDIENTE', 'HECHA')),
    CONSTRAINT ck_tareas_origen CHECK (origen IN ('MANUAL', 'IRIS')),
    -- Coherencia: una tarea HECHA tiene fecha de completado, y una PENDIENTE no
    CONSTRAINT ck_tareas_completada CHECK ((estado = 'HECHA') = (completada_en IS NOT NULL))
);

-- "Las tareas del evento, pendientes primero, por fecha límite"
CREATE INDEX ix_tareas_evento_estado ON tareas (evento_id, estado, fecha_limite);

-- Índice parcial para los recordatorios diarios (4.3): solo tareas pendientes con fecha
CREATE INDEX ix_tareas_pendientes_fecha ON tareas (fecha_limite)
    WHERE estado = 'PENDIENTE' AND fecha_limite IS NOT NULL;
