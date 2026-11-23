-- Día 4.3: notificaciones (la campanita) y el registro de recordatorios ya enviados.

-- 1) Notificaciones: un aviso para UNA persona
CREATE TABLE notificaciones (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id     UUID          NOT NULL,   -- el destinatario
    evento_id      UUID          NOT NULL,
    tipo           VARCHAR(40)   NOT NULL,
    mensaje        VARCHAR(300)  NOT NULL,
    -- A qué apunta el aviso (una tarea, una sesión, un documento, una invitación) para poder abrirlo
    referencia_id  UUID,
    creada_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    leida_en       TIMESTAMPTZ,

    CONSTRAINT fk_notificaciones_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_notificaciones_evento FOREIGN KEY (evento_id)
        REFERENCES eventos (id) ON DELETE CASCADE,
    CONSTRAINT ck_notificaciones_tipo CHECK (tipo IN (
        'INVITACION_RECIBIDA', 'MIEMBRO_NUEVO', 'SESION_NUEVA', 'TAREA_NUEVA', 'TAREA_ASIGNADA',
        'DOCUMENTO_INDEXADO', 'TAREA_VENCE_MANANA', 'TAREA_VENCE_HOY', 'TAREA_VENCIDA'))
);

-- "Mis notificaciones, de la más nueva a la más vieja"
CREATE INDEX ix_notificaciones_usuario ON notificaciones (usuario_id, creada_en DESC);

-- El numerito rojo de la campanita: solo las NO leídas (índice parcial, pequeño y rápido)
CREATE INDEX ix_notificaciones_no_leidas ON notificaciones (usuario_id) WHERE leida_en IS NULL;

-- 2) Recordatorios ya enviados: la "memoria" del despertador para NO repetir avisos.
--    La PK incluye la fecha límite: si alguien cambia la fecha, es un plazo nuevo y se vuelve a avisar.
--    Si el proceso corre dos veces el mismo día (o la app se reinicia), el INSERT choca con la PK
--    y ese recordatorio NO se envía otra vez.
CREATE TABLE recordatorios_enviados (
    tarea_id      UUID          NOT NULL,
    tipo          VARCHAR(40)   NOT NULL,
    fecha_limite  DATE          NOT NULL,
    enviado_en    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_recordatorios_enviados PRIMARY KEY (tarea_id, tipo, fecha_limite),
    CONSTRAINT fk_recordatorios_tarea FOREIGN KEY (tarea_id)
        REFERENCES tareas (id) ON DELETE CASCADE,
    CONSTRAINT ck_recordatorios_tipo CHECK (tipo IN ('TAREA_VENCE_MANANA', 'TAREA_VENCE_HOY', 'TAREA_VENCIDA'))
);
