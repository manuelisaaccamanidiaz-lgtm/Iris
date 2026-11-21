-- Historial y trazabilidad de las preguntas a Iris (decisión 3A).
CREATE TABLE consultas (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_id       UUID          NOT NULL,
    -- Si la persona borra su cuenta, la consulta queda como registro del evento (SET NULL)
    usuario_id      UUID,
    pregunta        TEXT          NOT NULL,
    respuesta       TEXT          NOT NULL,
    -- true = no había evidencia en los documentos (no se llamó a Groq, o Groq dijo "No encontré...")
    sin_evidencia   BOOLEAN       NOT NULL,
    -- Trazabilidad: con qué modelo se redactó y cuánto tardó
    modelo          VARCHAR(100),
    milisegundos    INTEGER       NOT NULL,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_consultas_evento FOREIGN KEY (evento_id)
        REFERENCES eventos (id) ON DELETE CASCADE,
    CONSTRAINT fk_consultas_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE SET NULL
);

-- "Mi historial en este evento, lo más reciente primero"
CREATE INDEX ix_consultas_evento_usuario ON consultas (evento_id, usuario_id, creado_en DESC);

-- Las fuentes que se le dieron al modelo en cada consulta (tabla aparte = normalizado, sin JSON).
-- Se COPIAN nombre, páginas y sección: si el documento se reindexa o se borra, la cita histórica sigue legible
CREATE TABLE consulta_fuentes (
    consulta_id     UUID          NOT NULL,
    numero          INTEGER       NOT NULL,   -- el [n] que vio el modelo
    fragmento_id    UUID,
    documento_id    UUID,
    documento       VARCHAR(255)  NOT NULL,
    paginas         VARCHAR(20)   NOT NULL,
    seccion         VARCHAR(300),
    -- ¿El modelo la citó de verdad en la respuesta?
    citada          BOOLEAN       NOT NULL,

    CONSTRAINT pk_consulta_fuentes PRIMARY KEY (consulta_id, numero),
    CONSTRAINT fk_fuentes_consulta FOREIGN KEY (consulta_id)
        REFERENCES consultas (id) ON DELETE CASCADE,
    CONSTRAINT fk_fuentes_fragmento FOREIGN KEY (fragmento_id)
        REFERENCES fragmentos (id) ON DELETE SET NULL,
    CONSTRAINT fk_fuentes_documento FOREIGN KEY (documento_id)
        REFERENCES documentos (id) ON DELETE SET NULL
);
