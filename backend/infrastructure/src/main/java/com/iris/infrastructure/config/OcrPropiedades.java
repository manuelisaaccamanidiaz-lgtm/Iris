package com.iris.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bloque "iris.ia.ocr": el OCR con Gemini visión.
 * @param modelo gemini-3.5-flash-lite: el Flash-Lite estable, pensado para mucho volumen a bajo costo
 *               (documentación de modelos de la Gemini API). Para transcribir no hace falta el más potente
 * @param dpi    resolución de la imagen de cada página: 150 basta para letra de documento y no pesa tanto
 */
@ConfigurationProperties(prefix = "iris.ia.ocr")
public record OcrPropiedades(String apiKey, String modelo, int dpi, Duration tiempoRespuesta) {
}
