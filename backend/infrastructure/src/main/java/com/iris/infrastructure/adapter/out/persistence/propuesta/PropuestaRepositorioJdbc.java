package com.iris.infrastructure.adapter.out.persistence.propuesta;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.iris.domain.agenda.TipoSesion;
import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.propuesta.EstadoPropuesta;
import com.iris.domain.propuesta.Propuesta;
import com.iris.domain.propuesta.PropuestaRepositorio;
import com.iris.domain.propuesta.TipoPropuesta;

/** Adaptador con SQL directo para la tabla propuestas (V12). */
@Component
class PropuestaRepositorioJdbc implements PropuestaRepositorio {

    private static final String COLUMNAS = """
            id, evento_id, documento_id, documento_nombre, tipo, estado, titulo, descripcion, fecha,
            hora_inicio, hora_fin, sala, tipo_sesion, cita, pagina_inicio, pagina_fin, creada_en,
            decidida_en, decidida_por, resultado_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    PropuestaRepositorioJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Propuesta guardar(Propuesta p) {
        if (p.id() == null) {
            UUID id = jdbc.queryForObject("""
                    INSERT INTO propuestas (evento_id, documento_id, documento_nombre, tipo, estado, titulo,
                        descripcion, fecha, hora_inicio, hora_fin, sala, tipo_sesion, cita, pagina_inicio,
                        pagina_fin, creada_en)
                    VALUES (:evento, :documento, :documentoNombre, :tipo, :estado, :titulo, :descripcion, :fecha,
                        :horaInicio, :horaFin, :sala, :tipoSesion, :cita, :paginaInicio, :paginaFin, :creadaEn)
                    RETURNING id
                    """, parametros(p), UUID.class);
            return conId(p, id);
        }
        // Al aceptar se guardan también los AJUSTES de la persona (título, fecha...).
        // "AND estado = 'PENDIENTE'": el segundo clic no actualiza nada -> 409 -> ROLLBACK de su tarea
        int filas = jdbc.update("""
                UPDATE propuestas SET estado = :estado, titulo = :titulo, descripcion = :descripcion,
                    fecha = :fecha, hora_inicio = :horaInicio, hora_fin = :horaFin, sala = :sala,
                    decidida_en = :decididaEn, decidida_por = :decididaPor, resultado_id = :resultado
                WHERE id = :id AND estado = 'PENDIENTE'
                """, parametros(p)
                .addValue("id", p.id())
                .addValue("decididaEn", p.decididaEn() == null ? null : Timestamp.from(p.decididaEn()))
                .addValue("decididaPor", p.decididaPor())
                .addValue("resultado", p.resultadoId()));
        if (filas == 0) {
            throw new ConflictoException("La propuesta ya no está pendiente");
        }
        return p;
    }

    @Override
    public Optional<Propuesta> buscarEnEvento(UUID propuestaId, UUID eventoId) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM propuestas WHERE id = :id AND evento_id = :evento",
                new MapSqlParameterSource().addValue("id", propuestaId).addValue("evento", eventoId),
                (rs, n) -> aDominio(rs)).stream().findFirst();
    }

    @Override
    public List<Propuesta> listarPorEvento(UUID eventoId, EstadoPropuesta estado) {
        String filtro = estado == null ? "" : "AND estado = :estado";
        return jdbc.query("SELECT " + COLUMNAS + " FROM propuestas WHERE evento_id = :evento " + filtro
                        + " ORDER BY creada_en DESC, pagina_inicio",
                new MapSqlParameterSource().addValue("evento", eventoId)
                        .addValue("estado", estado == null ? null : estado.name()),
                (rs, n) -> aDominio(rs));
    }

    @Override
    @Transactional // borrar las pendientes viejas + insertar las nuevas: todo o nada
    public List<Propuesta> reemplazarPendientesDelDocumento(UUID documentoId, List<Propuesta> nuevas) {
        jdbc.update("DELETE FROM propuestas WHERE documento_id = :documento AND estado = 'PENDIENTE'",
                new MapSqlParameterSource("documento", documentoId));
        List<Propuesta> guardadas = new ArrayList<>(nuevas.size());
        for (Propuesta p : nuevas) {
            guardadas.add(guardar(p));
        }
        return guardadas;
    }

    @Override
    public boolean recuperarDescartada(UUID propuestaId, UUID eventoId) {
        return jdbc.update("""
                UPDATE propuestas SET estado = 'PENDIENTE', decidida_en = NULL, decidida_por = NULL
                WHERE id = :id AND evento_id = :evento AND estado = 'DESCARTADA'
                """, new MapSqlParameterSource().addValue("id", propuestaId).addValue("evento", eventoId)) == 1;
    }

    private static MapSqlParameterSource parametros(Propuesta p) {
        return new MapSqlParameterSource()
                .addValue("evento", p.eventoId())
                .addValue("documento", p.documentoId())
                .addValue("documentoNombre", p.documentoNombre())
                .addValue("tipo", p.tipo().name())
                .addValue("estado", p.estado().name())
                .addValue("titulo", p.titulo())
                .addValue("descripcion", p.descripcion())
                .addValue("fecha", p.fecha())
                .addValue("horaInicio", p.horaInicio() == null ? null : Time.valueOf(p.horaInicio()))
                .addValue("horaFin", p.horaFin() == null ? null : Time.valueOf(p.horaFin()))
                .addValue("sala", p.sala())
                .addValue("tipoSesion", p.tipoSesion() == null ? null : p.tipoSesion().name())
                .addValue("cita", p.cita())
                .addValue("paginaInicio", p.paginaInicio())
                .addValue("paginaFin", p.paginaFin())
                .addValue("creadaEn", Timestamp.from(p.creadaEn()));
    }

    private static Propuesta conId(Propuesta p, UUID id) {
        return new Propuesta(id, p.eventoId(), p.documentoId(), p.documentoNombre(), p.tipo(), p.estado(),
                p.titulo(), p.descripcion(), p.fecha(), p.horaInicio(), p.horaFin(), p.sala(), p.tipoSesion(),
                p.cita(), p.paginaInicio(), p.paginaFin(), p.creadaEn(), p.decididaEn(), p.decididaPor(),
                p.resultadoId());
    }

    private static Propuesta aDominio(ResultSet rs) throws SQLException {
        Timestamp decidida = rs.getTimestamp("decidida_en");
        String tipoSesion = rs.getString("tipo_sesion");
        return new Propuesta(
                rs.getObject("id", UUID.class),
                rs.getObject("evento_id", UUID.class),
                rs.getObject("documento_id", UUID.class),
                rs.getString("documento_nombre"),
                TipoPropuesta.valueOf(rs.getString("tipo")),
                EstadoPropuesta.valueOf(rs.getString("estado")),
                rs.getString("titulo"),
                rs.getString("descripcion"),
                rs.getObject("fecha", LocalDate.class),
                rs.getObject("hora_inicio", LocalTime.class),
                rs.getObject("hora_fin", LocalTime.class),
                rs.getString("sala"),
                tipoSesion == null ? null : TipoSesion.valueOf(tipoSesion),
                rs.getString("cita"),
                rs.getInt("pagina_inicio"),
                rs.getInt("pagina_fin"),
                rs.getTimestamp("creada_en").toInstant(),
                decidida == null ? null : decidida.toInstant(),
                rs.getObject("decidida_por", UUID.class),
                rs.getObject("resultado_id", UUID.class));
    }
}
