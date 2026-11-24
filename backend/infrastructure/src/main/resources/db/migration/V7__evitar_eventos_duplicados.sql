-- Regla: un mismo creador no puede tener dos eventos con el mismo nombre
-- que empiecen el mismo día (sí puede repetir el nombre en otras fechas: "Congreso 2026-I", etc.).

-- 1) Los duplicados que YA existen (de las pruebas) impedirían crear el índice único.
--    NO los borramos (podrían tener documentos): los renombramos. Migrar datos sin perderlos.
--    ROW_NUMBER() numera las copias dentro de cada grupo: la más antigua es 1 y se queda igual.
WITH copias AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY creado_por, lower(nombre), fecha_inicio
                              ORDER BY creado_en) AS n
    FROM eventos
)
UPDATE eventos e
SET nombre = left(e.nombre, 130) || ' (duplicado ' || (c.n - 1) || ')'
FROM copias c
WHERE c.id = e.id AND c.n > 1;

-- 2) Índice único sobre una EXPRESIÓN: lower(nombre) hace que "Expo" y "EXPO" cuenten igual
CREATE UNIQUE INDEX ux_eventos_creador_nombre_fecha
    ON eventos (creado_por, lower(nombre), fecha_inicio);
