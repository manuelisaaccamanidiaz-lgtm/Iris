package com.iris.application.propuesta;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.iris.application.port.out.AnalizadorDocumentos.PropuestaIA;
import com.iris.domain.agenda.TipoSesion;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.documento.TextoFragmento;
import com.iris.domain.evento.Evento;
import com.iris.domain.propuesta.Propuesta;
import com.iris.domain.propuesta.TipoPropuesta;

/**
 * El "control de calidad" de lo que dijo la IA (versión 2, Día 6). Nunca confiamos en un modelo
 * de lenguaje a ciegas:
 * <ol>
 *   <li>el fragmento citado debe EXISTIR;</li>
 *   <li>la cita debe salir DE VERDAD de ese fragmento. v2: la IA a veces cambia una palabra o un signo
 *       al copiar, y la v1 la descartaba por eso. Ahora buscamos en el fragmento la ORACIÓN REAL que más
 *       se parece a lo que citó la IA: si comparten al menos el 60 % de las palabras, la propuesta se queda
 *       con esa oración real (texto del documento, no de la IA). Si no se parece a nada, se descarta;</li>
 *   <li>v2: nada repetido con lo que YA existe (tareas, sesiones, propuestas aceptadas) ni dentro del lote;</li>
 *   <li>v2 (decisión de Isaac): lo DESCARTADO no se repite... salvo que el documento le ponga un plazo:
 *       entonces vuelve, con una nota que lo explica;</li>
 *   <li>v2 (decisión de Isaac): un plazo que YA PASÓ se propone para HOY, con una nota;</li>
 *   <li>una SESION sin horas o fuera de las fechas del evento se convierte en TAREA.</li>
 * </ol>
 * Clase sin estado y sin dependencias: se prueba fácil (ValidadorPropuestasTest).
 */
public final class ValidadorPropuestas {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter FECHA_TEXTO = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.of("es"));
    private static final double PARECIDO_MINIMO = 0.6;
    private static final int MAX_CITA = 300;

    private ValidadorPropuestas() {
    }

    /**
     * Lo que ya sabe el evento, para no repetir.
     * @param existentes títulos de tareas, sesiones y propuestas aceptadas o pendientes de OTROS documentos
     * @param descartados títulos de propuestas descartadas
     */
    public record Contexto(LocalDate hoy, Set<String> existentes, Set<String> descartados) {

        public static Contexto de(LocalDate hoy, List<String> existentes, List<String> descartados) {
            return new Contexto(hoy, normalizados(existentes), normalizados(descartados));
        }

        private static Set<String> normalizados(List<String> titulos) {
            return titulos.stream().map(ValidadorPropuestas::normalizar).collect(Collectors.toSet());
        }
    }

    /**
     * @param motivos por qué se descartó cada una ("ya existe": 3, "cita inventada": 1...). Va al log:
     *                "descartó 6" no ayuda a nadie; "6 porque ya existían" sí (era lo que pasaba).
     */
    public record Resultado(List<Propuesta> validas, Map<String, Integer> motivos) {

        public int descartadas() {
            return motivos.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    public static Resultado validar(List<PropuestaIA> ias, List<TextoFragmento> fragmentos, UUID documentoId,
                                    String documentoNombre, Evento evento, Contexto contexto) {
        List<Propuesta> validas = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        Map<String, Integer> motivos = new LinkedHashMap<>();
        for (PropuestaIA ia : ias == null ? List.<PropuestaIA>of() : ias) {
            Propuesta p = convertir(ia, fragmentos, documentoId, documentoNombre, evento, contexto, motivos);
            if (p == null) {
                continue; // convertir() ya anotó el motivo
            }
            if (!vistas.add(normalizar(p.titulo()))) {
                descartar(motivos, "repetida en el mismo lote");
                continue;
            }
            validas.add(p);
        }
        return new Resultado(validas, motivos);
    }

    /** Anota el motivo y devuelve null (= descartada), para escribir {@code return descartar(...)}. */
    private static Propuesta descartar(Map<String, Integer> motivos, String motivo) {
        motivos.merge(motivo, 1, Integer::sum);
        return null;
    }

    /** null = descartada. */
    static Propuesta convertir(PropuestaIA ia, List<TextoFragmento> fragmentos, UUID documentoId,
                               String documentoNombre, Evento evento, Contexto contexto,
                               Map<String, Integer> motivos) {
        if (ia == null || ia.titulo() == null || ia.fragmento() < 1 || ia.fragmento() > fragmentos.size()) {
            return descartar(motivos, "sin título o fragmento inexistente");
        }
        TextoFragmento fuente = fragmentos.get(ia.fragmento() - 1);
        String citaReal = citaVerificada(ia.cita(), fuente.contenido());
        if (citaReal == null) {
            return descartar(motivos, "cita que no está en el documento"); // no se parece a nada del texto
        }
        String titulo = normalizar(ia.titulo());
        if (contexto.existentes().contains(titulo)) {
            return descartar(motivos, "ya existe en el evento"); // tarea, sesión o propuesta
        }

        TipoPropuesta tipo;
        try {
            tipo = TipoPropuesta.valueOf(ia.tipo().trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return descartar(motivos, "tipo desconocido");
        }
        LocalDate fecha = fecha(ia.fecha());
        LocalTime inicio = hora(ia.horaInicio());
        LocalTime fin = hora(ia.horaFin());
        List<String> notas = new ArrayList<>();

        boolean yaDescartada = contexto.descartados().contains(titulo);
        if (yaDescartada) {
            if (fecha == null) {
                return descartar(motivos, "ya la descartaste"); // y sin plazo: respetamos la decisión
            }
            notas.add("La descartaste antes, pero el documento fija un plazo para esto.");
        }
        if (fecha != null && fecha.isBefore(contexto.hoy())) {
            notas.add("El documento pedía hacerlo para el " + fecha.format(FECHA_TEXTO) + ": ese plazo ya pasó.");
            fecha = contexto.hoy();
            inicio = null; // una sesión en el pasado no se puede agendar: queda como tarea urgente
        }

        boolean sesionPosible = fecha != null && inicio != null && fin != null && fin.isAfter(inicio)
                && !fecha.isBefore(evento.fechaInicio()) && !fecha.isAfter(evento.fechaFin());
        if (tipo == TipoPropuesta.SESION && !sesionPosible) {
            tipo = TipoPropuesta.TAREA;
        }
        boolean esSesion = tipo == TipoPropuesta.SESION;
        String descripcion = conNotas(ia.descripcion(), notas);
        try {
            return Propuesta.crear(evento.id(), documentoId, documentoNombre, tipo, ia.titulo(), descripcion,
                    fecha, esSesion ? inicio : null, esSesion ? fin : null, esSesion ? ia.sala() : null,
                    esSesion ? tipoSesion(ia.tipoSesion()) : null, citaReal,
                    fuente.paginaInicio(), fuente.paginaFin());
        } catch (ReglaDeNegocioException e) {
            return descartar(motivos, "datos inválidos (" + e.getMessage() + ")");
        }
    }

    /**
     * La oración REAL del fragmento que respalda lo que citó la IA, o null si ninguna se parece lo suficiente.
     * Así la cita que ve la persona siempre es texto del documento, aunque la IA la haya copiado mal.
     */
    static String citaVerificada(String citaIa, String texto) {
        Set<String> palabrasIa = palabras(citaIa == null ? "" : citaIa.replace("...", " "));
        if (palabrasIa.size() < 3) {
            return null; // demasiado corta para demostrar algo ("el pago")
        }
        String mejor = null;
        double mejorParecido = 0;
        // Corta en ". " / ": " / "; " SOLO si lo que sigue empieza en mayúscula (no parte "$30.000" ni "art. 3")
        for (String oracion : texto.split("(?<=[.;:!?])\\s+(?=[\\p{Lu}¿¡«\"(])|\\n+")) {
            Set<String> enOracion = palabras(oracion);
            long comunes = palabrasIa.stream().filter(enOracion::contains).count();
            double parecido = (double) comunes / palabrasIa.size();
            if (parecido > mejorParecido) {
                mejorParecido = parecido;
                mejor = oracion.trim();
            }
        }
        if (mejor == null || mejorParecido < PARECIDO_MINIMO) {
            return null;
        }
        return mejor.length() > MAX_CITA ? mejor.substring(0, MAX_CITA - 3) + "..." : mejor;
    }

    /** Las palabras "con contenido" (3 letras o más), en limpio. */
    private static Set<String> palabras(String s) {
        return Arrays.stream(normalizar(s).split(" ")).filter(p -> p.length() >= 3).collect(Collectors.toSet());
    }

    /** "Pagaré el 30%  del ANTICIPO." -> "pagare el 30 del anticipo" */
    static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String conNotas(String descripcion, List<String> notas) {
        if (notas.isEmpty()) {
            return descripcion;
        }
        String base = descripcion == null || descripcion.isBlank() ? "" : descripcion.trim() + " ";
        return base + String.join(" ", notas);
    }

    private static LocalDate fecha(String s) {
        try {
            return s == null || s.isBlank() ? null : LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalTime hora(String s) {
        try {
            return s == null || s.isBlank() ? null : LocalTime.parse(s.trim(), HORA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static TipoSesion tipoSesion(String s) {
        try {
            return TipoSesion.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return TipoSesion.OTRO;
        }
    }
}
