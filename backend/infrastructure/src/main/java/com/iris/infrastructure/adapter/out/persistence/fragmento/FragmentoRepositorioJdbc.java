package com.iris.infrastructure.adapter.out.persistence.fragmento;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.iris.domain.documento.Fragmento;
import com.iris.domain.documento.FragmentoRepositorio;
import com.iris.domain.documento.TextoFragmento;
import com.iris.infrastructure.adapter.out.persistence.VectorPg;

/**
 * Adaptador con JdbcTemplate (SQL directo) en vez de JPA. ¿Por qué? JPA/Hibernate no conoce el
 * tipo "vector" de pgvector. Con SQL le pasamos el vector como texto "[0.12,-0.5,...]" y
 * PostgreSQL lo convierte con CAST(? AS vector). Simple y sin dependencias extra.
 */
@Component
class FragmentoRepositorioJdbc implements FragmentoRepositorio {

    private static final String INSERTAR = """
            INSERT INTO fragmentos (documento_id, evento_id, orden, pagina_inicio, pagina_fin,
                                    seccion, contenido, tokens, embedding)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS vector))
            """;

    private final JdbcTemplate jdbc;

    FragmentoRepositorioJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** DELETE + INSERT en una transacción: o quedan todos los fragmentos nuevos, o ninguno. */
    @Override
    @Transactional
    public void reemplazarDelDocumento(UUID documentoId, List<Fragmento> fragmentos) {
        jdbc.update("DELETE FROM fragmentos WHERE documento_id = ?", documentoId);
        // batchUpdate: manda todos los INSERT juntos (mucho más rápido que uno por uno)
        jdbc.batchUpdate(INSERTAR, fragmentos, 50, (ps, f) -> {
            ps.setObject(1, f.documentoId());
            ps.setObject(2, f.eventoId());
            ps.setInt(3, f.orden());
            ps.setInt(4, f.paginaInicio());
            ps.setInt(5, f.paginaFin());
            ps.setString(6, f.seccion());
            ps.setString(7, f.contenido());
            ps.setInt(8, f.tokens());
            ps.setString(9, VectorPg.aTexto(f.embedding()));
        });
    }

    @Override
    public int contarPorDocumento(UUID documentoId) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM fragmentos WHERE documento_id = ?", Integer.class, documentoId);
        return total == null ? 0 : total;
    }

    @Override
    public List<TextoFragmento> listarTextosDelDocumento(UUID documentoId) {
        // Sin la columna embedding: 1536 números por fila que aquí no sirven para nada
        return jdbc.query("""
                SELECT orden, pagina_inicio, pagina_fin, seccion, contenido, tokens
                FROM fragmentos WHERE documento_id = ? ORDER BY orden
                """, (rs, n) -> new TextoFragmento(rs.getInt("orden"), rs.getInt("pagina_inicio"),
                rs.getInt("pagina_fin"), rs.getString("seccion"), rs.getString("contenido"), rs.getInt("tokens")),
                documentoId);
    }
}
