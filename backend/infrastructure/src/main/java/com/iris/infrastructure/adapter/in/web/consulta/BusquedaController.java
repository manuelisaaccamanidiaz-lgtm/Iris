package com.iris.infrastructure.adapter.in.web.consulta;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.consulta.BuscarFragmentos;
import com.iris.application.port.out.BuscadorFragmentos.FragmentoEncontrado;

/**
 * "Rayos X" de Iris: muestra QUÉ fragmentos encontraría para una pregunta y POR QUÉ
 * (puesto en cada juez y puntaje RRF), sin llamar a Groq. Útil para depurar y para la sustentación.
 */
@RestController
class BusquedaController {

    private final BuscarFragmentos buscarFragmentos;

    BusquedaController(BuscarFragmentos buscarFragmentos) {
        this.buscarFragmentos = buscarFragmentos;
    }

    @GetMapping("/api/eventos/{eventoId}/busqueda")
    List<Resultado> buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                           @RequestParam("q") String pregunta) {
        UUID usuarioId = UUID.fromString(jwt.getSubject());
        return buscarFragmentos.ejecutar(usuarioId, eventoId, pregunta).stream().map(Resultado::de).toList();
    }

    record Resultado(String documento, String paginas, String seccion, Integer rangoVector,
                     Integer rangoTexto, Double similitud, double rrf, String vistaPrevia) {

        static Resultado de(FragmentoEncontrado f) {
            String vista = f.contenido().length() > 160 ? f.contenido().substring(0, 160) + "…" : f.contenido();
            Double sim = f.similitud() == null ? null : Math.round(f.similitud() * 1000) / 1000.0;
            return new Resultado(f.nombreArchivo(), f.paginas(), f.seccion(), f.rangoVector(),
                    f.rangoTexto(), sim, Math.round(f.puntajeRrf() * 10000) / 10000.0, vista);
        }
    }
}
