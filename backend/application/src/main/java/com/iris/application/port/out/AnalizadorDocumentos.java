package com.iris.application.port.out;

import java.util.List;

/**
 * Puerto de salida: un modelo de lenguaje que lee un documento y devuelve propuestas
 * ESTRUCTURADAS (no texto libre). Hoy lo implementa Groq con JSON Schema.
 *
 * <p>Ojo: lo que devuelve es "lo que dijo la IA", en bruto y SIN validar (por eso todo es texto).
 * Quien lo usa (GenerarPropuestas) desconfía y lo valida campo por campo.
 */
public interface AnalizadorDocumentos {

    List<PropuestaIA> proponer(String instrucciones, String contenido);

    /**
     * Una propuesta tal como la escribe la IA.
     * @param fragmento el número [n] del fragmento donde está la evidencia
     * @param cita      la frase que, según la IA, copió textualmente de ese fragmento
     */
    record PropuestaIA(String tipo, String titulo, String descripcion, String fecha, String horaInicio,
                       String horaFin, String sala, String tipoSesion, int fragmento, String cita) {
    }
}
