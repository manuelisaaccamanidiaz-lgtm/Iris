package com.iris.application.propuesta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.iris.application.port.out.ContadorTokens;
import com.iris.domain.documento.TextoFragmento;
import com.iris.domain.evento.Evento;

/**
 * El prompt de las propuestas (hermano de PromptRag). Las reglas van en el mensaje de SISTEMA;
 * el evento, la fecha de hoy y los fragmentos numerados van en el mensaje de USUARIO.
 */
public final class PromptPropuestas {

    /** Decisión de Isaac: "todo el documento, con un tope". Cuida la cuota gratis de Groq. */
    public static final int TOPE_TOKENS = 6000;

    public static final String INSTRUCCIONES = """
            Eres Iris, una asistente de IA para organizadores de eventos. Lees UN documento del evento \
            (contrato, cronograma, reglamento...) y propones acciones concretas para el equipo.

            Reglas:
            1. Propón SOLO lo que el documento dice de forma explícita: pagos, entregas, plazos, confirmaciones, \
            reuniones, montajes, actividades. NUNCA inventes datos que no estén en los fragmentos.
            2. TAREA = algo que una persona del equipo debe HACER (pagar, enviar, confirmar, contratar, entregar). \
            Si el documento da un plazo, ponlo en "fecha" con formato AAAA-MM-DD; si no, deja "fecha" en "".
            3. SESION = una actividad que OCURRE en una fecha con hora de inicio y hora de fin (montaje, reunión, \
            conferencia, taller). Solo si el documento da la fecha Y las dos horas (formato HH:MM). \
            Si falta alguna de las tres, propónla como TAREA.
            4. Fechas relativas ("30 días antes del evento", "el día de la inauguración"): calcúlalas con las \
            fechas del evento que te doy. Si no puedes calcularlas con seguridad, deja "fecha" en "".
            5. Cada propuesta indica "fragmento" (el número [n] donde está la evidencia) y "cita": una frase \
            COPIADA TEXTUALMENTE de ese fragmento, de máximo 200 caracteres. Sin cita textual no hay propuesta.
            6. "titulo": corto (máximo 80 caracteres), en español, empezando con un verbo. "descripcion": una \
            frase con el detalle importante (monto, cantidad, condición, penalidad).
            7. Máximo 10 propuestas, las más importantes, sin repetir. Si el documento no exige ninguna \
            acción, devuelve la lista vacía.
            8. "tipoSesion": CONFERENCIA, TALLER, PANEL, REUNION, LOGISTICA u OTRO (en una TAREA, "").
            9. "sala": solo si el documento la nombra; si no, "".
            10. Responde ÚNICAMENTE con el JSON pedido, sin texto antes ni después.
            """;

    private PromptPropuestas() {
    }

    /** Los fragmentos en orden hasta el tope de tokens (siempre al menos el primero). */
    public static List<TextoFragmento> seleccionar(List<TextoFragmento> fragmentos, ContadorTokens contador) {
        List<TextoFragmento> elegidos = new ArrayList<>();
        int total = 0;
        for (TextoFragmento f : fragmentos) {
            int tokens = f.tokens() > 0 ? f.tokens() : contador.contar(f.contenido());
            if (!elegidos.isEmpty() && total + tokens > TOPE_TOKENS) {
                break;
            }
            elegidos.add(f);
            total += tokens;
        }
        return elegidos;
    }

    public static String contenido(Evento evento, String documento, List<TextoFragmento> fragmentos,
                                   LocalDate hoy) {
        StringBuilder sb = new StringBuilder();
        sb.append("EVENTO: ").append(evento.nombre())
                .append("\nFECHAS DEL EVENTO: del ").append(evento.fechaInicio()).append(" al ").append(evento.fechaFin());
        if (evento.lugar() != null) {
            sb.append("\nLUGAR: ").append(evento.lugar());
        }
        sb.append("\nHOY ES: ").append(hoy)
                .append("\nDOCUMENTO: ").append(documento)
                .append("\n\nFRAGMENTOS DEL DOCUMENTO:\n");
        for (int i = 0; i < fragmentos.size(); i++) {
            TextoFragmento f = fragmentos.get(i);
            sb.append("\n[").append(i + 1).append("] (páginas ").append(f.paginaInicio()).append("-")
                    .append(f.paginaFin()).append(")\n").append(f.contenido()).append("\n");
        }
        return sb.toString();
    }
}
