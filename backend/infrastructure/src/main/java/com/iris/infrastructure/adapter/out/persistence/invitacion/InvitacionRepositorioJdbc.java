package com.iris.infrastructure.adapter.out.persistence.invitacion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.evento.RolEnEvento;
import com.iris.domain.invitacion.EstadoInvitacion;
import com.iris.domain.invitacion.Invitacion;
import com.iris.domain.invitacion.InvitacionRecibida;
import com.iris.domain.invitacion.InvitacionRepositorio;

/** Adaptador con SQL directo para la tabla invitaciones (V10). */
@Component
class InvitacionRepositorioJdbc implements InvitacionRepositorio {

    private static final String COLUMNAS = """
            i.id, i.evento_id, i.email, i.rol, i.estado, i.invitado_por, i.creada_en, i.expira_en, i.cerrada_en
            """;

    private final NamedParameterJdbcTemplate jdbc;

    InvitacionRepositorioJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Invitacion guardar(Invitacion inv) {
        if (inv.id() == null) {
            UUID id = jdbc.queryForObject("""
                    INSERT INTO invitaciones (evento_id, email, rol, estado, invitado_por, creada_en, expira_en)
                    VALUES (:evento, :email, :rol, :estado, :invitadoPor, :creadaEn, :expiraEn)
                    RETURNING id
                    """, new MapSqlParameterSource()
                    .addValue("evento", inv.eventoId())
                    .addValue("email", inv.email())
                    .addValue("rol", inv.rol().name())
                    .addValue("estado", inv.estado().name())
                    .addValue("invitadoPor", inv.invitadoPor())
                    .addValue("creadaEn", Timestamp.from(inv.creadaEn()))
                    .addValue("expiraEn", Timestamp.from(inv.expiraEn())), UUID.class);
            return new Invitacion(id, inv.eventoId(), inv.email(), inv.rol(), inv.estado(), inv.invitadoPor(),
                    inv.creadaEn(), inv.expiraEn(), inv.cerradaEn());
        }
        // Lo único que cambia de una invitación es su estado (y cuándo se cerró).
        // "AND estado = 'PENDIENTE'": si otra petición la cerró un instante antes (doble clic),
        // no se actualiza nada y avisamos con un 409 en vez de pisar su respuesta
        int filas = jdbc.update("""
                UPDATE invitaciones SET estado = :estado, cerrada_en = :cerradaEn
                WHERE id = :id AND estado = 'PENDIENTE'
                """, new MapSqlParameterSource()
                .addValue("id", inv.id())
                .addValue("estado", inv.estado().name())
                .addValue("cerradaEn", inv.cerradaEn() == null ? null : Timestamp.from(inv.cerradaEn())));
        if (filas == 0) {
            throw new ConflictoException("La invitación ya no está pendiente");
        }
        return inv;
    }

    @Override
    public Optional<Invitacion> buscarPorId(UUID id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM invitaciones i WHERE i.id = :id",
                new MapSqlParameterSource("id", id), (rs, n) -> aDominio(rs)).stream().findFirst();
    }

    @Override
    public Optional<Invitacion> buscarPendiente(UUID eventoId, String email) {
        return jdbc.query("""
                SELECT %s FROM invitaciones i
                WHERE i.evento_id = :evento AND i.email = :email AND i.estado = 'PENDIENTE'
                """.formatted(COLUMNAS), new MapSqlParameterSource()
                .addValue("evento", eventoId)
                .addValue("email", email), (rs, n) -> aDominio(rs)).stream().findFirst();
    }

    @Override
    public List<Invitacion> listarPorEvento(UUID eventoId) {
        return jdbc.query("""
                SELECT %s FROM invitaciones i WHERE i.evento_id = :evento ORDER BY i.creada_en DESC
                """.formatted(COLUMNAS), new MapSqlParameterSource("evento", eventoId), (rs, n) -> aDominio(rs));
    }

    @Override
    public List<InvitacionRecibida> listarPendientesPara(String email) {
        // LEFT JOIN usuarios: si quien invitó borró su cuenta, la invitación sigue (invitado_por = NULL)
        return jdbc.query("""
                SELECT %s, e.nombre AS evento_nombre, e.fecha_inicio, e.fecha_fin, u.nombre AS invitado_por_nombre
                FROM invitaciones i
                JOIN eventos e ON e.id = i.evento_id
                LEFT JOIN usuarios u ON u.id = i.invitado_por
                WHERE i.email = :email AND i.estado = 'PENDIENTE'
                ORDER BY i.creada_en DESC
                """.formatted(COLUMNAS), new MapSqlParameterSource("email", email),
                (rs, n) -> new InvitacionRecibida(aDominio(rs), rs.getString("evento_nombre"),
                        rs.getObject("fecha_inicio", LocalDate.class),
                        rs.getObject("fecha_fin", LocalDate.class),
                        rs.getString("invitado_por_nombre")));
    }

    private static Invitacion aDominio(ResultSet rs) throws SQLException {
        return new Invitacion(
                rs.getObject("id", UUID.class),
                rs.getObject("evento_id", UUID.class),
                rs.getString("email"),
                RolEnEvento.valueOf(rs.getString("rol")),
                EstadoInvitacion.valueOf(rs.getString("estado")),
                rs.getObject("invitado_por", UUID.class),
                instante(rs, "creada_en"),
                instante(rs, "expira_en"),
                instante(rs, "cerrada_en"));
    }

    private static Instant instante(ResultSet rs, String columna) throws SQLException {
        Timestamp t = rs.getTimestamp(columna);
        return t == null ? null : t.toInstant();
    }
}
