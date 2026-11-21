package com.iris.infrastructure.adapter.out.persistence.consulta;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.iris.domain.consulta.Consulta;
import com.iris.domain.consulta.ConsultaRepositorio;
import com.iris.domain.consulta.FuenteCitada;

/** Adaptador con SQL directo: una consulta y sus fuentes se guardan juntas (transacción). */
@Component
class ConsultaRepositorioJdbc implements ConsultaRepositorio {

    private final NamedParameterJdbcTemplate jdbc;

    ConsultaRepositorioJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Consulta guardar(Consulta c) {
        // RETURNING: PostgreSQL nos devuelve el id generado en el mismo INSERT
        UUID id = jdbc.queryForObject("""
                INSERT INTO consultas (evento_id, usuario_id, pregunta, respuesta, sin_evidencia,
                                       modelo, milisegundos, creado_en)
                VALUES (:evento, :usuario, :pregunta, :respuesta, :sinEvidencia, :modelo, :ms, :creadoEn)
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("evento", c.eventoId())
                .addValue("usuario", c.usuarioId())
                .addValue("pregunta", c.pregunta())
                .addValue("respuesta", c.respuesta())
                .addValue("sinEvidencia", c.sinEvidencia())
                .addValue("modelo", c.modelo())
                .addValue("ms", c.milisegundos())
                .addValue("creadoEn", Timestamp.from(c.creadoEn())), UUID.class);

        for (FuenteCitada f : c.fuentes()) {
            jdbc.update("""
                    INSERT INTO consulta_fuentes (consulta_id, numero, fragmento_id, documento_id,
                                                  documento, paginas, seccion, citada)
                    VALUES (:consulta, :numero, :fragmento, :documento, :nombre, :paginas, :seccion, :citada)
                    """, new MapSqlParameterSource()
                    .addValue("consulta", id)
                    .addValue("numero", f.numero())
                    .addValue("fragmento", f.fragmentoId())
                    .addValue("documento", f.documentoId())
                    .addValue("nombre", f.documento())
                    .addValue("paginas", f.paginas())
                    .addValue("seccion", f.seccion())
                    .addValue("citada", f.citada()));
        }
        return new Consulta(id, c.eventoId(), c.usuarioId(), c.pregunta(), c.respuesta(), c.sinEvidencia(),
                c.fuentes(), c.modelo(), c.milisegundos(), c.creadoEn());
    }

    @Override
    public List<Consulta> listarDelUsuario(UUID eventoId, UUID usuarioId, int limite) {
        // 1) Las consultas (sin fuentes todavía), en orden
        Map<UUID, Consulta> porId = new LinkedHashMap<>();
        jdbc.query("""
                SELECT id, evento_id, usuario_id, pregunta, respuesta, sin_evidencia, modelo, milisegundos, creado_en
                FROM consultas
                WHERE evento_id = :evento AND usuario_id = :usuario
                ORDER BY creado_en DESC
                LIMIT :limite
                """, new MapSqlParameterSource().addValue("evento", eventoId).addValue("usuario", usuarioId)
                .addValue("limite", limite), rs -> {
                    UUID id = rs.getObject("id", UUID.class);
                    porId.put(id, new Consulta(id, rs.getObject("evento_id", UUID.class),
                            rs.getObject("usuario_id", UUID.class), rs.getString("pregunta"),
                            rs.getString("respuesta"), rs.getBoolean("sin_evidencia"), List.of(),
                            rs.getString("modelo"), rs.getInt("milisegundos"),
                            rs.getTimestamp("creado_en").toInstant()));
                });
        if (porId.isEmpty()) {
            return List.of();
        }

        // 2) TODAS sus fuentes en UNA sola consulta (IN (:ids)) para evitar el problema "N+1"
        Map<UUID, List<FuenteCitada>> fuentes = new LinkedHashMap<>();
        jdbc.query("""
                SELECT consulta_id, numero, fragmento_id, documento_id, documento, paginas, seccion, citada
                FROM consulta_fuentes
                WHERE consulta_id IN (:ids)
                ORDER BY numero
                """, new MapSqlParameterSource("ids", porId.keySet()), rs -> {
                    fuentes.computeIfAbsent(rs.getObject("consulta_id", UUID.class), k -> new ArrayList<>())
                            .add(new FuenteCitada(rs.getInt("numero"), rs.getObject("fragmento_id", UUID.class),
                                    rs.getObject("documento_id", UUID.class), rs.getString("documento"),
                                    rs.getString("paginas"), rs.getString("seccion"), rs.getBoolean("citada")));
                });

        // 3) Unimos cada consulta con sus fuentes
        return porId.values().stream()
                .map(c -> new Consulta(c.id(), c.eventoId(), c.usuarioId(), c.pregunta(), c.respuesta(),
                        c.sinEvidencia(), fuentes.getOrDefault(c.id(), List.of()), c.modelo(),
                        c.milisegundos(), c.creadoEn()))
                .toList();
    }
}
