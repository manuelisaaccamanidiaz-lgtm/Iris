package com.iris.application.propuesta;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.iris.application.port.out.AnalizadorDocumentos.PropuestaIA;
import com.iris.domain.agenda.TipoSesion;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.documento.TextoFragmento;
import com.iris.domain.evento.Evento;
import com.iris.domain.propuesta.Propuesta;
import com.iris.domain.propuesta.TipoPropuesta;

/**
 * El "control de calidad" de lo que dijo la IA. Nunca confiamos en un modelo de lenguaje a ciegas:
 * <ol>
 *   <li>el fragmento citado debe EXISTIR;</li>
 *   <li>la cita debe aparecer DE VERDAD en ese fragmento (anti-alucinación: si la IA se inventó
 *       la frase, la propuesta se descarta);</li>
 *   <li>las fechas y horas deben poder leerse;</li>
 *   <li>una SESION sin horas o fuera de las fechas del evento se convierte en TAREA (no se pierde la
 *       información, pero tampoco entra a la agenda algo imposible);</li>
 *   <li>sin repetidos.</li>
 * </ol>
 * Clase sin estado y sin dependencias: se prueba fácil (ValidadorPropuestasTest).
 */
public final class ValidadorPropuestas {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm");

    private ValidadorPropuestas() {
    }

    public record Resultado(List<Propuesta> validas, int descartadas) {
    }

    public static Resultado validar(List<PropuestaIA> ias, List<TextoFragmento> fragmentos, UUID documentoId,
                                    String documentoNombre, Evento evento) {
        List<Propuesta> validas = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        int descartadas = 0;
        for (PropuestaIA ia : ias == null ? List.<PropuestaIA>of() : ias) {
            Propuesta p = convertir(ia, fragmentos, documentoId, documentoNombre, evento);
            if (p == null || !vistas.add(p.tipo() + "|" + normalizar(p.titulo()))) {
                descartadas++;
                continue;
            }
            validas.add(p);
        }
        return new Resultado(validas, descartadas);
    }

    /** null = descartada. */
    static Propuesta convertir(PropuestaIA ia, List<TextoFragmento> fragmentos, UUID documentoId,
                               String documentoNombre, Evento evento) {
        if (ia == null || ia.fragmento() < 1 || ia.fragmento() > fragmentos.size()) {
            return null; // cita un fragmento que no existe
        }
        TextoFragmento fuente = fragmentos.get(ia.fragmento() - 1);
        if (!citaEsTextual(ia.cita(), fuente.contenido())) {
            return null; // la frase no está en el documento: la IA la inventó o la parafraseó
        }
        TipoPropuesta tipo;
        try {
            tipo = TipoPropuesta.valueOf(ia.tipo().trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return null;
        }
        LocalDate fecha = fecha(ia.fecha());
        LocalTime inicio = hora(ia.horaInicio());
        LocalTime fin = hora(ia.horaFin());
        boolean sesionPosible = fecha != null && inicio != null && fin != null && fin.isAfter(inicio)
                && !fecha.isBefore(evento.fechaInicio()) && !fecha.isAfter(evento.fechaFin());
        if (tipo == TipoPropuesta.SESION && !sesionPosible) {
            tipo = TipoPropuesta.TAREA; // regla 4 de esta clase
        }
        boolean esSesion = tipo == TipoPropuesta.SESION;
        try {
            return Propuesta.crear(evento.id(), documentoId, documentoNombre, tipo, ia.titulo(), ia.descripcion(),
                    fecha, esSesion ? inicio : null, esSesion ? fin : null, esSesion ? ia.sala() : null,
                    esSesion ? tipoSesion(ia.tipoSesion()) : null, ia.cita(),
                    fuente.paginaInicio(), fuente.paginaFin());
        } catch (ReglaDeNegocioException e) {
            return null; // p. ej. sin título
        }
    }

    /** ¿La cita aparece en el texto? Comparando "en limpio": sin tildes, mayúsculas ni signos. */
    static boolean citaEsTextual(String cita, String texto) {
        String c = normalizar(cita == null ? "" : cita.replace("...", " "));
        if (c.length() < 12) {
            return false; // demasiado corta para demostrar algo ("el pago")
        }
        return normalizar(texto).contains(c);
    }

    /** "Pagaré el 30%  del ANTICIPO." -> "pagare el 30 del anticipo" */
    static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
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
