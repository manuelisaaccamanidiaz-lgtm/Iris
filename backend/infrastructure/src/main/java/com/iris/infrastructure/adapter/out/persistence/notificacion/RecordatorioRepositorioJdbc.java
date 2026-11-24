package com.iris.infrastructure.adapter.out.persistence.notificacion;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.iris.domain.notificacion.RecordatorioRepositorio;
import com.iris.domain.notificacion.TipoNotificacion;

/** La memoria del despertador. Un solo INSERT decide si el aviso es nuevo: idempotente. */
@Component
class RecordatorioRepositorioJdbc implements RecordatorioRepositorio {

    private final NamedParameterJdbcTemplate jdbc;

    RecordatorioRepositorioJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean registrarSiEsNuevo(UUID tareaId, TipoNotificacion tipo, LocalDate fechaLimite) {
        // ON CONFLICT DO NOTHING: si la PK (tarea, tipo, fecha) ya existe, PostgreSQL no inserta
        // NI lanza error; simplemente devuelve 0 filas. 1 fila = nuevo; 0 = ya se había enviado
        int filas = jdbc.update("""
                INSERT INTO recordatorios_enviados (tarea_id, tipo, fecha_limite)
                VALUES (:tarea, :tipo, :fecha)
                ON CONFLICT DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("tarea", tareaId)
                .addValue("tipo", tipo.name())
                .addValue("fecha", fechaLimite));
        return filas == 1;
    }
}
