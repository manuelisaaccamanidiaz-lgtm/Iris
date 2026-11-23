package com.iris.infrastructure.adapter.out.persistence.notificacion;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.iris.domain.notificacion.Notificacion;
import com.iris.domain.notificacion.NotificacionRepositorio;
import com.iris.domain.notificacion.TipoNotificacion;

/** La campanita con SQL directo. Toda consulta filtra por usuario_id: nadie ve las de otro. */
@Component
class NotificacionRepositorioJdbc implements NotificacionRepositorio {

    private static final RowMapper<Notificacion> FILA = (rs, n) -> new Notificacion(
            rs.getObject("id", UUID.class),
            rs.getObject("usuario_id", UUID.class),
            rs.getObject("evento_id", UUID.class),
            TipoNotificacion.valueOf(rs.getString("tipo")),
            rs.getString("mensaje"),
            rs.getObject("referencia_id", UUID.class),
            rs.getTimestamp("creada_en").toInstant(),
            rs.getTimestamp("leida_en") == null ? null : rs.getTimestamp("leida_en").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    NotificacionRepositorioJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Notificacion guardar(Notificacion n) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO notificaciones (usuario_id, evento_id, tipo, mensaje, referencia_id, creada_en)
                VALUES (:usuario, :evento, :tipo, :mensaje, :referencia, :creadaEn)
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("usuario", n.usuarioId())
                .addValue("evento", n.eventoId())
                .addValue("tipo", n.tipo().name())
                .addValue("mensaje", n.mensaje())
                .addValue("referencia", n.referenciaId())
                .addValue("creadaEn", Timestamp.from(n.creadaEn())), UUID.class);
        return new Notificacion(id, n.usuarioId(), n.eventoId(), n.tipo(), n.mensaje(), n.referenciaId(),
                n.creadaEn(), n.leidaEn());
    }

    @Override
    public List<Notificacion> listarDe(UUID usuarioId, boolean soloNoLeidas, int limite) {
        String filtro = soloNoLeidas ? "AND leida_en IS NULL" : "";
        return jdbc.query("""
                SELECT id, usuario_id, evento_id, tipo, mensaje, referencia_id, creada_en, leida_en
                FROM notificaciones
                WHERE usuario_id = :usuario %s
                ORDER BY creada_en DESC
                LIMIT :limite
                """.formatted(filtro), new MapSqlParameterSource()
                .addValue("usuario", usuarioId)
                .addValue("limite", limite), FILA);
    }

    @Override
    public int contarNoLeidas(UUID usuarioId) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*)::int FROM notificaciones WHERE usuario_id = :usuario AND leida_en IS NULL",
                new MapSqlParameterSource("usuario", usuarioId), Integer.class);
        return total == null ? 0 : total;
    }

    @Override
    public boolean marcarLeida(UUID notificacionId, UUID usuarioId, Instant cuando) {
        // "AND usuario_id": si no es tuya, actualiza 0 filas -> 404 (no revelamos que existe)
        return jdbc.update("""
                UPDATE notificaciones SET leida_en = COALESCE(leida_en, :cuando)
                WHERE id = :id AND usuario_id = :usuario
                """, new MapSqlParameterSource()
                .addValue("id", notificacionId)
                .addValue("usuario", usuarioId)
                .addValue("cuando", Timestamp.from(cuando))) == 1;
    }

    @Override
    public int marcarTodasLeidas(UUID usuarioId, Instant cuando) {
        return jdbc.update("""
                UPDATE notificaciones SET leida_en = :cuando WHERE usuario_id = :usuario AND leida_en IS NULL
                """, new MapSqlParameterSource()
                .addValue("usuario", usuarioId)
                .addValue("cuando", Timestamp.from(cuando)));
    }
}
