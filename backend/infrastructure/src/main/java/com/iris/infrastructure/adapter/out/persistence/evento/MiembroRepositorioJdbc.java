package com.iris.infrastructure.adapter.out.persistence.evento;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.iris.domain.evento.MiembroEvento;
import com.iris.domain.evento.MiembroRepositorio;
import com.iris.domain.evento.RolEnEvento;

/** Adaptador con SQL directo para el equipo (evento_miembros + nombre y email de usuarios). */
@Component
class MiembroRepositorioJdbc implements MiembroRepositorio {

    private static final String SELECT = """
            SELECT m.evento_id, m.usuario_id, u.nombre, u.email, m.rol, m.unido_en, m.archivado_en
            FROM evento_miembros m
            JOIN usuarios u ON u.id = m.usuario_id
            """;

    private static final RowMapper<MiembroEvento> FILA = (rs, n) -> new MiembroEvento(
            rs.getObject("evento_id", UUID.class),
            rs.getObject("usuario_id", UUID.class),
            rs.getString("nombre"),
            rs.getString("email"),
            RolEnEvento.valueOf(rs.getString("rol")),
            rs.getTimestamp("unido_en").toInstant(),
            rs.getTimestamp("archivado_en") == null ? null : rs.getTimestamp("archivado_en").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    MiembroRepositorioJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<MiembroEvento> listar(UUID eventoId) {
        // Propietarios primero, luego editores y lectores; dentro de cada rol, por nombre
        return jdbc.query(SELECT + """
                WHERE m.evento_id = :evento
                ORDER BY CASE m.rol WHEN 'PROPIETARIO' THEN 0 WHEN 'EDITOR' THEN 1 ELSE 2 END, u.nombre
                """, new MapSqlParameterSource("evento", eventoId), FILA);
    }

    @Override
    public Optional<MiembroEvento> buscar(UUID eventoId, UUID usuarioId) {
        return jdbc.query(SELECT + "WHERE m.evento_id = :evento AND m.usuario_id = :usuario",
                ids(eventoId, usuarioId), FILA).stream().findFirst();
    }

    @Override
    public boolean existeConEmail(UUID eventoId, String email) {
        // lower(u.email) aprovecha el índice único de la V2
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM evento_miembros m JOIN usuarios u ON u.id = m.usuario_id
                               WHERE m.evento_id = :evento AND lower(u.email) = :email)
                """, new MapSqlParameterSource().addValue("evento", eventoId).addValue("email", email),
                Boolean.class));
    }

    @Override
    public void agregar(UUID eventoId, UUID usuarioId, RolEnEvento rol) {
        // Si ya existe la fila (doble clic), la PK de la V6 lo impide -> DataIntegrityViolation -> 409
        jdbc.update("INSERT INTO evento_miembros (evento_id, usuario_id, rol) VALUES (:evento, :usuario, :rol)",
                ids(eventoId, usuarioId).addValue("rol", rol.name()));
    }

    @Override
    public void cambiarRol(UUID eventoId, UUID usuarioId, RolEnEvento nuevoRol) {
        jdbc.update("UPDATE evento_miembros SET rol = :rol WHERE evento_id = :evento AND usuario_id = :usuario",
                ids(eventoId, usuarioId).addValue("rol", nuevoRol.name()));
    }

    @Override
    public void quitar(UUID eventoId, UUID usuarioId) {
        // La FK de tareas (V9) pone responsable_id en NULL automáticamente
        jdbc.update("DELETE FROM evento_miembros WHERE evento_id = :evento AND usuario_id = :usuario",
                ids(eventoId, usuarioId));
    }

    @Override
    public void archivar(UUID eventoId, UUID usuarioId, Instant cuando) {
        jdbc.update("UPDATE evento_miembros SET archivado_en = :cuando WHERE evento_id = :evento AND usuario_id = :usuario",
                ids(eventoId, usuarioId).addValue("cuando", Timestamp.from(cuando)));
    }

    private static MapSqlParameterSource ids(UUID eventoId, UUID usuarioId) {
        return new MapSqlParameterSource().addValue("evento", eventoId).addValue("usuario", usuarioId);
    }
}
