-- Activa la extensión pgvector en esta base de datos.
-- La imagen de Docker ya la trae instalada; aquí la "conectamos".
-- IF NOT EXISTS: si ya estaba activa, no falla.
CREATE EXTENSION IF NOT EXISTS vector;
