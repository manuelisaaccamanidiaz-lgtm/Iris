package com.iris.infrastructure.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bloque "iris.ia.ocr": el OCR con Gemini visión.
 * @param modelos en ORDEN de preferencia. Primero gemini-3.5-flash-lite (el estable para mucho volumen a bajo
 *                costo); si está saturado (503) o llegamos al límite (429), se intenta con el siguiente.
 *                v2: la demanda alta de un modelo dejó documentos en ERROR; con respaldo, uno solo no basta para caer
 * @param dpi    resolución de la imagen de cada página: 150 basta para letra de documento y no pesa tanto
 */
@ConfigurationProperties(prefix = "iris.ia.ocr")
public record OcrPropiedades(String apiKey, List<String> modelos, int dpi, Duration tiempoRespuesta) {
}
