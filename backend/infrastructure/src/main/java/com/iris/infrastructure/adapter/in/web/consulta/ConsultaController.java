package com.iris.infrastructure.adapter.in.web.consulta;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.iris.application.consulta.ConsultarIris;
import com.iris.application.consulta.HistorialConsultas;
import com.iris.application.consulta.OyenteConsulta;
import com.iris.domain.consulta.Consulta;
import com.iris.domain.consulta.FuenteCitada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** "Pregúntale a Iris": chat con y sin streaming, e historial. */
@RestController
@RequestMapping("/api/eventos/{eventoId}/consultas")
class ConsultaController {

    private static final Logger log = LoggerFactory.getLogger(ConsultaController.class);
    private static final long TIEMPO_MAXIMO_MS = 90_000; // si Groq no termina en 90 s, se cierra

    private final ConsultarIris consultarIris;
    private final HistorialConsultas historial;
    private final TaskExecutor chatExecutor;

    ConsultaController(ConsultarIris consultarIris, HistorialConsultas historial,
                       @Qualifier("chatExecutor") TaskExecutor chatExecutor) {
        this.consultarIris = consultarIris;
        this.historial = historial;
        this.chatExecutor = chatExecutor;
    }

    record SolicitudConsulta(@NotBlank String pregunta) {
    }

    /** Sin streaming: espera la respuesta completa (útil para pruebas y clientes simples). */
    @PostMapping
    RespuestaConsulta preguntar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                                @Valid @RequestBody SolicitudConsulta s) {
        return RespuestaConsulta.de(consultarIris.ejecutar(usuario(jwt), eventoId, s.pregunta()));
    }

    /**
     * CON streaming (Server-Sent Events). El método devuelve el SseEmitter DE INMEDIATO
     * (la conexión queda abierta) y el trabajo sigue en un hilo "chat-N", que va escribiendo eventos:
     * fuentes -> trozo, trozo, trozo... -> fin   (o "error" si algo falla a mitad)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter preguntarEnVivo(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                               @Valid @RequestBody SolicitudConsulta s) {
        UUID usuarioId = usuario(jwt);
        // Validamos ANTES de abrir el stream: así un no-miembro recibe un 404 normal
        consultarIris.validar(usuarioId, eventoId, s.pregunta());

        SseEmitter emisor = new SseEmitter(TIEMPO_MAXIMO_MS);
        chatExecutor.execute(() -> {
            try {
                consultarIris.ejecutar(usuarioId, eventoId, s.pregunta(), new OyenteSse(emisor));
                emisor.complete();
            } catch (Exception e) {
                log.error("Falló la consulta en streaming", e);
                try {
                    emisor.send(SseEmitter.event().name("error")
                            .data(Map.of("mensaje", "Iris no pudo responder en este momento. Intenta de nuevo."),
                                    MediaType.APPLICATION_JSON));
                } catch (IOException | IllegalStateException ignorada) {
                    // el cliente ya se fue: no hay a quién avisar
                }
                emisor.complete();
            }
        });
        return emisor;
    }

    /** Mi historial en este evento. */
    @GetMapping
    List<RespuestaConsulta> historial(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId) {
        return historial.ejecutar(usuario(jwt), eventoId).stream().map(RespuestaConsulta::de).toList();
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject()); // el "quién" SIEMPRE sale del token
    }

    /** Adaptador del oyente: convierte cada aviso del caso de uso en un evento SSE con nombre. */
    private static final class OyenteSse implements OyenteConsulta {

        private final SseEmitter emisor;

        OyenteSse(SseEmitter emisor) {
            this.emisor = emisor;
        }

        @Override
        public void fuentes(List<FuenteCitada> fuentes) {
            enviar("fuentes", fuentes.stream().map(Fuente::de).toList());
        }

        @Override
        public void trozo(String texto) {
            // Como JSON {"t": "..."}: así los saltos de línea del texto no rompen el formato SSE
            enviar("trozo", Map.of("t", texto));
        }

        @Override
        public void fin(Consulta consulta) {
            enviar("fin", RespuestaConsulta.de(consulta));
        }

        private void enviar(String nombre, Object datos) {
            try {
                emisor.send(SseEmitter.event().name(nombre).data(datos, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                // El cliente cerró la conexión: cortamos el proceso (no tiene sentido seguir generando)
                throw new UncheckedIOException("El cliente cerró la conexión", e);
            }
        }
    }

    record Fuente(int numero, String documento, String paginas, String seccion, boolean citada) {

        static Fuente de(FuenteCitada f) {
            return new Fuente(f.numero(), f.documento(), f.paginas(), f.seccion(), f.citada());
        }
    }

    /** Solo se muestran las fuentes CITADAS (las demás se le dieron al modelo pero no las usó). */
    record RespuestaConsulta(UUID id, String pregunta, String respuesta, boolean sinEvidencia,
                             List<Fuente> fuentes, String modelo, int milisegundos, Instant creadoEn) {

        static RespuestaConsulta de(Consulta c) {
            return new RespuestaConsulta(c.id(), c.pregunta(), c.respuesta(), c.sinEvidencia(),
                    c.fuentesCitadas().stream().map(Fuente::de).toList(), c.modelo(), c.milisegundos(),
                    c.creadoEn());
        }
    }
}
