package com.iris.infrastructure.adapter.out.persistence.busqueda;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.BuscadorFragmentos;
import com.iris.infrastructure.adapter.out.persistence.VectorPg;

/**
 * Adaptador: búsqueda híbrida en UNA consulta SQL.
 * NamedParameterJdbcTemplate permite parámetros con nombre (:evento) en vez de "?",
 * mucho más legible cuando un parámetro se repite varias veces.
 */
@Component
class BuscadorHibridoPg implements BuscadorFragmentos {

    /** Cuántos candidatos toma CADA juez antes de fusionar. */
    private static final int CANDIDATOS = 20;

    /** Constante de RRF: 60 es el valor estándar del artículo original (amortigua los primeros puestos). */
    private static final int K_RRF = 60;

    private static final String SQL = """
            WITH vec AS (
                -- Juez 1: SIGNIFICADO. <=> = distancia coseno (0 = idénticos)
                SELECT f.id,
                       f.embedding <=> CAST(:vector AS vector) AS distancia,
                       ROW_NUMBER() OVER (ORDER BY f.embedding <=> CAST(:vector AS vector)) AS rango
                FROM fragmentos f
                WHERE f.evento_id = :evento
                ORDER BY f.embedding <=> CAST(:vector AS vector)
                LIMIT :candidatos
            ),
            consulta AS (
                -- plainto_tsquery une las palabras con & (Y). Lo cambiamos por | (O)
                -- para no exigir que estén TODAS; ts_rank_cd premia a quien tenga más
                SELECT to_tsquery('spanish',
                         replace(plainto_tsquery('spanish', :pregunta)::text, '&', '|')) AS q
            ),
            txt AS (
                -- Juez 2: PALABRAS. @@ = "este tsvector coincide con esta consulta"
                SELECT f.id,
                       ROW_NUMBER() OVER (ORDER BY ts_rank_cd(f.contenido_tsv, c.q) DESC) AS rango
                FROM fragmentos f, consulta c
                WHERE f.evento_id = :evento AND f.contenido_tsv @@ c.q
                ORDER BY ts_rank_cd(f.contenido_tsv, c.q) DESC
                LIMIT :candidatos
            ),
            fusion AS (
                -- RRF. FULL OUTER JOIN: un fragmento puede estar en una sola de las dos listas
                SELECT COALESCE(v.id, t.id) AS id,
                       v.distancia,
                       v.rango AS rango_vector,
                       t.rango AS rango_texto,
                       COALESCE(1.0 / (:k + v.rango), 0) + COALESCE(1.0 / (:k + t.rango), 0) AS rrf
                FROM vec v FULL OUTER JOIN txt t ON v.id = t.id
            )
            SELECT fu.id, f.documento_id, d.nombre_archivo, f.pagina_inicio, f.pagina_fin, f.seccion,
                   f.contenido, fu.distancia, fu.rango_vector, fu.rango_texto, fu.rrf
            FROM fusion fu
            JOIN fragmentos f ON f.id = fu.id
            JOIN documentos d ON d.id = f.documento_id
            WHERE d.estado = 'INDEXADO'
              -- Defensa en profundidad: aunque el caso de uso ya lo verificó, el SQL tampoco
              -- devuelve nada si el usuario no es miembro del evento
              AND EXISTS (SELECT 1 FROM evento_miembros m
                          WHERE m.evento_id = :evento AND m.usuario_id = :usuario)
            ORDER BY fu.rrf DESC
            LIMIT :limite
            """;

    private final NamedParameterJdbcTemplate jdbc;

    BuscadorHibridoPg(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<FragmentoEncontrado> buscar(UUID eventoId, UUID usuarioId, String pregunta,
                                            float[] vectorPregunta, int limite) {
        var parametros = new MapSqlParameterSource()
                .addValue("vector", VectorPg.aTexto(vectorPregunta))
                .addValue("evento", eventoId)
                .addValue("usuario", usuarioId)
                .addValue("pregunta", pregunta)
                .addValue("candidatos", CANDIDATOS)
                .addValue("k", K_RRF)
                .addValue("limite", limite);

        return jdbc.query(SQL, parametros, (rs, fila) -> {
            Double distancia = (Double) rs.getObject("distancia");
            return new FragmentoEncontrado(
                    rs.getObject("id", UUID.class),
                    rs.getObject("documento_id", UUID.class),
                    rs.getString("nombre_archivo"),
                    rs.getInt("pagina_inicio"),
                    rs.getInt("pagina_fin"),
                    rs.getString("seccion"),
                    rs.getString("contenido"),
                    distancia == null ? null : 1.0 - distancia,  // similitud coseno
                    (Integer) rs.getObject("rango_vector", Integer.class),
                    (Integer) rs.getObject("rango_texto", Integer.class),
                    rs.getDouble("rrf"));
        });
    }
}
