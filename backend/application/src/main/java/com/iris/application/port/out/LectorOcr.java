package com.iris.application.port.out;

import java.util.List;

/**
 * Puerto de salida: OCR (reconocimiento óptico de caracteres). Convierte en TEXTO las páginas que son
 * solo una IMAGEN (un contrato firmado que pasaron por el escáner). Hoy lo implementa Gemini visión.
 * Requisito: "un trabajo asíncrono extrae texto y ejecuta OCR para páginas escaneadas".
 */
public interface LectorOcr {

    /**
     * @param numerosPagina las páginas a leer (empiezan en 1), en el orden que se quieren
     * @return el texto de cada página, en el MISMO orden que numerosPagina
     */
    List<String> leer(byte[] pdf, List<Integer> numerosPagina);
}
