-- Día 4.2: invitaciones, varios propietarios y "salir" de un evento terminado.

-- 1) Varios propietarios (decisión de Isaac). Soltamos el índice "un solo propietario" de la V6.
--    El propietario ORIGINAL es eventos.creado_por: solo él nombra o quita propietarios.
--    Esa regla vive en el dominio (EventoDeMiembro), porque depende de quién hace la petición.
DROP INDEX ux_evento_un_propietario;

-- 2) El creador puede "salirse" de un evento YA TERMINADO para que no le estorbe en su lista.
--    No borramos su fila (trazabilidad): solo marcamos cuándo lo archivó.
ALTER TABLE evento_miembros ADD COLUMN archivado_en TIMESTAMPTZ;

-- 3) Invitaciones por email
CREATE TABLE invitaciones (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_id     UUID          NOT NULL,
    -- Email y no usuario_id: el invitado puede NO tener cuenta todavía.
    -- Se guarda normalizado (minúsculas) para comparar sin sorpresas
    email         VARCHAR(255)  NOT NULL,
    rol           VARCHAR(20)   NOT NULL,
    estado        VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    invitado_por  UUID,
    creada_en     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    expira_en     TIMESTAMPTZ   NOT NULL,
    -- Cuándo dejó de estar PENDIENTE (aceptada, rechazada, cancelada o vencida)
    cerrada_en    TIMESTAMPTZ,

    CONSTRAINT fk_invitaciones_evento FOREIGN KEY (evento_id)
        REFERENCES eventos (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitaciones_invitado_por FOREIGN KEY (invitado_por)
        REFERENCES usuarios (id) ON DELETE SET NULL,
    CONSTRAINT ck_invitaciones_rol CHECK (rol IN ('PROPIETARIO', 'EDITOR', 'LECTOR')),
    CONSTRAINT ck_invitaciones_estado
        CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'CANCELADA', 'VENCIDA')),
    CONSTRAINT ck_invitaciones_email_minusculas CHECK (email = lower(email)),
    CONSTRAINT ck_invitaciones_expira CHECK (expira_en > creada_en),
    -- El mismo truco de tareas.completada_en: PENDIENTE <=> sin fecha de cierre
    CONSTRAINT ck_invitaciones_cerrada CHECK ((estado = 'PENDIENTE') = (cerrada_en IS NULL))
);

-- Índice único PARCIAL: máximo UNA invitación pendiente por (evento, email).
-- Las rechazadas, canceladas o vencidas no estorban: se puede volver a invitar
CREATE UNIQUE INDEX ux_invitaciones_una_pendiente ON invitaciones (evento_id, email)
    WHERE estado = 'PENDIENTE';

-- "Mi bandeja": las pendientes dirigidas a MI email (el índice de arriba empieza por evento_id
-- y no sirve para buscar solo por email)
CREATE INDEX ix_invitaciones_bandeja ON invitaciones (email) WHERE estado = 'PENDIENTE';

-- Para el proceso diario del 4.3, que marca como VENCIDA lo que nadie tocó
CREATE INDEX ix_invitaciones_expira ON invitaciones (expira_en) WHERE estado = 'PENDIENTE';
