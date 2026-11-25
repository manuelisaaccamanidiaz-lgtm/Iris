package com.iris.infrastructure.adapter.out.ia;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.time.Duration;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iris.application.port.out.IaNoDisponibleException;
import com.iris.application.port.out.LectorOcr;
import com.iris.infrastructure.config.OcrPropiedades;

/**
 * Adaptador: OCR con Gemini visión (decisión de Isaac: opción A).
 * <pre>
 * página del PDF --PDFBox--> imagen PNG (en grises) --Base64--> Gemini "transcribe" --> texto
 * </pre>
 * Llamamos a la API REST de Gemini directamente con RestClient (spring-web, que ya teníamos), tal como
 * la documenta Google: POST .../models/{modelo}:generateContent con la imagen en "inline_data".
 * Así no sumamos otra librería solo para esto.
 */
@Component
class LectorOcrGemini implements LectorOcr {

    private static final Logger log = LoggerFactory.getLogger(LectorOcrGemini.class);

    /** Las mismas reglas de siempre: copiar, no inventar. El texto alimenta las citas de Iris. */
    static final String INSTRUCCION = """
            Transcribe EXACTAMENTE todo el texto de esta página escaneada, en orden de lectura.
            No resumas, no traduzcas, no corrijas, no agregues comentarios ni formato Markdown.
            Si una palabra es ilegible, escribe [ilegible]. Si la página no tiene texto, responde vacío.""";

    private final RestClient cliente;
    private final OcrPropiedades propiedades;

    LectorOcrGemini(OcrPropiedades propiedades) {
        this.propiedades = propiedades;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(propiedades.tiempoRespuesta()); // sin esto, una llamada colgada no terminaría
        this.cliente = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .requestFactory(fabrica)
                .build();
    }

    @Override
    public List<String> leer(byte[] pdf, List<Integer> numerosPagina) {
        // Se abre el PDF UNA vez para todas las páginas (abrirlo por página sería lento)
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFRenderer pintor = new PDFRenderer(documento);
            List<String> textos = new ArrayList<>(numerosPagina.size());
            for (int numero : numerosPagina) {
                // PDFBox cuenta las páginas desde 0; nosotros desde 1 (como las citas)
                BufferedImage imagen = pintor.renderImageWithDPI(numero - 1, propiedades.dpi(), ImageType.GRAY);
                textos.add(transcribir(png(imagen), numero));
            }
            return textos;
        } catch (IOException e) {
            throw new UncheckedIOException("No pude convertir las páginas escaneadas en imágenes", e);
        }
    }

    private String transcribir(byte[] png, int numero) {
        Peticion peticion = new Peticion(
                List.of(new Contenido(List.of(
                        Parte.imagen(Base64.getEncoder().encodeToString(png)),
                        Parte.texto(INSTRUCCION)))),
                Map.of("temperature", 0)); // transcribir, no crear
        try {
            return llamar(peticion);
        } catch (HttpClientErrorException.TooManyRequests e) {
            // 429 = límite por minuto del plan gratis: esperamos un poco y lo intentamos UNA vez más
            log.warn("Gemini OCR: límite por minuto en la página {}; reintento en 10 s", numero);
            esperar(10_000);
            try {
                return llamar(peticion);
            } catch (RestClientException otra) {
                throw new IaNoDisponibleException("Gemini no pudo leer la página " + numero + " (OCR)", otra);
            }
        } catch (RestClientException e) {
            throw new IaNoDisponibleException("Gemini no pudo leer la página " + numero + " (OCR): " + e.getMessage(), e);
        }
    }

    private String llamar(Peticion peticion) {
        Respuesta r = cliente.post()
                .uri("/models/{modelo}:generateContent", propiedades.modelo())
                .header("x-goog-api-key", propiedades.apiKey()) // la clave va en la cabecera, nunca en la URL
                .body(peticion)
                .retrieve()
                .body(Respuesta.class);
        return r == null ? "" : r.texto();
    }

    private static byte[] png(BufferedImage imagen) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --- La forma del JSON, según el ejemplo REST de la documentación de Gemini ---

    record Peticion(List<Contenido> contents, Map<String, Object> generationConfig) {
    }

    record Contenido(List<Parte> parts) {
    }

    /** Una parte es texto O imagen. NON_NULL: la que no aplica no se envía (Gemini rechaza parts con las dos). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Parte(String text, @JsonProperty("inline_data") Imagen inlineData) {

        static Parte texto(String t) {
            return new Parte(t, null);
        }

        static Parte imagen(String base64) {
            return new Parte(null, new Imagen("image/png", base64));
        }
    }

    record Imagen(@JsonProperty("mime_type") String mimeType, String data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(List<Candidato> candidates) {

        /** El texto de la primera respuesta (puede venir partido en varias "parts"). */
        String texto() {
            if (candidates == null || candidates.isEmpty() || candidates.get(0).content() == null
                    || candidates.get(0).content().parts() == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            candidates.get(0).content().parts().forEach(p -> sb.append(p.text() == null ? "" : p.text()));
            return sb.toString();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidato(ContenidoRespuesta content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContenidoRespuesta(List<ParteRespuesta> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ParteRespuesta(String text) {
    }
}
