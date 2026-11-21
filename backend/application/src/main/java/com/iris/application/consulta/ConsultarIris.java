package com.iris.application.consulta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.iris.application.port.out.BuscadorFragmentos.FragmentoEncontrado;
import com.iris.application.port.out.RedactorRespuestas;
import com.iris.domain.consulta.Consulta;
import com.iris.domain.consulta.ConsultaRepositorio;
import com.iris.domain.consulta.FuenteCitada;

/**
 * Caso de uso estrella: el flujo de consulta completo del profe (pasos 1 a 4) + trazabilidad.
 * "Pregúntale a Iris."
 */
public class ConsultarIris {

    /** Captura los números de cita: "[1]", "[12]"... */
    private static final Pattern CITA = Pattern.compile("\\[(\\d+)]");

    private final BuscarFragmentos buscarFragmentos;
    private final RedactorRespuestas redactor;
    private final ConsultaRepositorio consultas;
    /** Similitud mínima para aceptar un fragmento que NO comparte palabras con la pregunta. */
    private final double umbralSimilitud;

    public ConsultarIris(BuscarFragmentos buscarFragmentos, RedactorRespuestas redactor,
                         ConsultaRepositorio consultas, double umbralSimilitud) {
        this.buscarFragmentos = buscarFragmentos;
        this.redactor = redactor;
        this.consultas = consultas;
        this.umbralSimilitud = umbralSimilitud;
    }

    public Consulta ejecutar(UUID usuarioId, UUID eventoId, String pregunta, OyenteConsulta oyente) {
        long inicio = System.currentTimeMillis();

        // 1. Membresía + embedding + búsqueda híbrida (lanza 404 si no es miembro)
        List<FragmentoEncontrado> encontrados = buscarFragmentos.ejecutar(usuarioId, eventoId, pregunta);

        // 2. Filtro de EVIDENCIA (decisión 4A): ¿comparte palabras o es suficientemente parecido?
        List<FragmentoEncontrado> evidencia = encontrados.stream()
                .filter(f -> f.rangoTexto() != null
                        || (f.similitud() != null && f.similitud() >= umbralSimilitud))
                .toList();

        if (evidencia.isEmpty()) {
            // Sin evidencia: NO llamamos a Groq. Ahorramos cuota y eliminamos la tentación de inventar
            oyente.fuentes(List.of());
            oyente.trozo(PromptRag.SIN_RESPUESTA);
            return terminar(usuarioId, eventoId, pregunta, PromptRag.SIN_RESPUESTA, true,
                    List.of(), null, inicio, oyente);
        }

        // 3. Fuentes numeradas: el [n] que verá el modelo es el mismo que verá el usuario
        List<FuenteCitada> fuentes = new ArrayList<>();
        for (int i = 0; i < evidencia.size(); i++) {
            FragmentoEncontrado f = evidencia.get(i);
            fuentes.add(new FuenteCitada(i + 1, f.fragmentoId(), f.documentoId(), f.nombreArchivo(),
                    f.paginas(), f.seccion(), false));
        }
        oyente.fuentes(fuentes);

        // 4. Groq redacta EN VIVO: cada trozo sale al cliente y también lo acumulamos
        StringBuilder texto = new StringBuilder();
        redactor.redactarEnVivo(PromptRag.instrucciones(), PromptRag.mensaje(pregunta, evidencia), trozo -> {
            texto.append(trozo);
            oyente.trozo(trozo);
        });

        // 5. Validar citas: quitamos las que apuntan a fuentes inexistentes y marcamos las usadas
        String respuesta = limpiarCitasInvalidas(texto.toString().strip(), fuentes.size());
        boolean sinEvidencia = respuesta.startsWith(PromptRag.SIN_RESPUESTA);
        Set<Integer> citadas = sinEvidencia ? Set.of() : numerosCitados(respuesta);
        List<FuenteCitada> marcadas = fuentes.stream()
                .map(f -> f.marcarCitada(citadas.contains(f.numero())))
                .toList();

        // 6. Guardar en el historial y avisar el final
        return terminar(usuarioId, eventoId, pregunta, respuesta, sinEvidencia, marcadas,
                redactor.modelo(), inicio, oyente);
    }

    /** Validar ANTES de empezar (lo usa el streaming para responder 404/400 a tiempo). */
    public void validar(UUID usuarioId, UUID eventoId, String pregunta) {
        buscarFragmentos.validar(usuarioId, eventoId, pregunta);
    }

    /** Versión sin streaming (para pruebas y clientes simples). */
    public Consulta ejecutar(UUID usuarioId, UUID eventoId, String pregunta) {
        return ejecutar(usuarioId, eventoId, pregunta, OyenteConsulta.SILENCIOSO);
    }

    private Consulta terminar(UUID usuarioId, UUID eventoId, String pregunta, String respuesta,
                              boolean sinEvidencia, List<FuenteCitada> fuentes, String modelo,
                              long inicio, OyenteConsulta oyente) {
        int ms = (int) (System.currentTimeMillis() - inicio);
        Consulta guardada = consultas.guardar(new Consulta(null, eventoId, usuarioId, pregunta.strip(),
                respuesta, sinEvidencia, fuentes, modelo, ms, Instant.now()));
        oyente.fin(guardada);
        return guardada;
    }

    /** "[1] ... [7]" con 3 fuentes -> se borra "[7]": nunca mostramos una cita que no existe. */
    static String limpiarCitasInvalidas(String texto, int totalFuentes) {
        Matcher m = CITA.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            m.appendReplacement(sb, (n >= 1 && n <= totalFuentes) ? Matcher.quoteReplacement(m.group()) : "");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static Set<Integer> numerosCitados(String texto) {
        Set<Integer> numeros = new TreeSet<>();
        Matcher m = CITA.matcher(texto);
        while (m.find()) {
            numeros.add(Integer.parseInt(m.group(1)));
        }
        return numeros;
    }
}
