package com.iris.infrastructure.adapter.out.ia;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iris.application.port.out.AnalizadorDocumentos;
import com.iris.application.port.out.IaNoDisponibleException;
import com.iris.infrastructure.config.ChatPropiedades;

/**
 * Adaptador: propuestas ESTRUCTURADAS con Groq. Decisión de Isaac: "doble cinturón".
 * <ol>
 *   <li><b>BeanOutputConverter</b> (Spring AI) genera el JSON Schema a partir de los records de abajo,
 *       añade al prompt las instrucciones de formato y, al final, convierte el texto JSON en objetos Java.</li>
 *   <li><b>JSON Schema Mode</b> de Groq (response_format = json_schema): el MODELO queda obligado a
 *       responder con esa forma exacta.</li>
 * </ol>
 * Si Groq rechazara el JSON Schema (por ejemplo, un modelo que no lo soporte), reintentamos UNA vez con
 * JSON Object Mode: garantiza JSON válido, y la forma la guían las instrucciones del conversor.
 */
@Component
class AnalizadorDocumentosGroq implements AnalizadorDocumentos {

    private static final Logger log = LoggerFactory.getLogger(AnalizadorDocumentosGroq.class);

    /** gpt-oss "piensa" antes de responder y eso también gasta tokens: 800 (los del chat) no alcanzan. */
    private static final int MAX_TOKENS = 4000;

    private final ChatModel chat;
    private final ChatPropiedades propiedades;
    /** Para PEDIR: genera el esquema estricto (todos los campos "required") que le mandamos a Groq. */
    private final BeanOutputConverter<RespuestaIA> conversor = new BeanOutputConverter<>(RespuestaIA.class);
    /**
     * Para LEER: los mismos campos, pero ninguno obligatorio. El bug del 500: el JSON Schema falló, el
     * respaldo (JSON Object Mode) solo garantiza JSON VÁLIDO, no la forma, y la IA omitió "horaInicio".
     * Con "required" Jackson rechazaba la respuesta ENTERA por un campo de una sola propuesta.
     * Ahora un campo que falta llega como null y el validador decide (ya sabe qué hacer con nulls).
     */
    private final BeanOutputConverter<RespuestaFlexible> lector = new BeanOutputConverter<>(RespuestaFlexible.class);

    AnalizadorDocumentosGroq(ChatModel chat, ChatPropiedades propiedades) {
        this.chat = chat;
        this.propiedades = propiedades;
    }

    @Override
    public List<PropuestaIA> proponer(String instrucciones, String contenido) {
        // getFormat(): "responde en JSON que cumpla este esquema: {...}" (lo escribe Spring AI)
        String sistema = instrucciones + "\n\n" + conversor.getFormat();
        String json;
        try {
            json = llamar(sistema, contenido, OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA);
        } catch (RuntimeException e) {
            log.warn("Groq rechazó JSON Schema ({}); reintento con JSON Object Mode", e.getMessage());
            try {
                json = llamar(sistema, contenido, OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT);
            } catch (RuntimeException otra) {
                // Fallaron los DOS intentos: casi siempre es el límite de tokens por minuto de Groq (429)
                log.error("Groq no respondió a las propuestas", otra);
                throw new IaNoDisponibleException("Groq no respondió: " + otra.getMessage(), otra);
            }
        }
        RespuestaFlexible respuesta;
        try {
            respuesta = lector.convert(json);
        } catch (RuntimeException e) {
            // Ni siquiera es el JSON que esperábamos: es un fallo de la IA, no un bug nuestro
            log.error("La IA respondió algo que no se pudo leer como propuestas", e);
            throw new IaNoDisponibleException("Respuesta ilegible de la IA", e);
        }
        if (respuesta == null || respuesta.propuestas() == null) {
            return List.of();
        }
        return respuesta.propuestas().stream().filter(java.util.Objects::nonNull).map(ItemFlexible::aPuerto).toList();
    }

    private String llamar(String sistema, String contenido, OpenAiChatModel.ResponseFormat.Type tipo) {
        var formato = OpenAiChatModel.ResponseFormat.builder().type(tipo);
        if (tipo == OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA) {
            formato.jsonSchema(conversor.getJsonSchema());
        }
        // Repetimos URL, clave y modelo de Groq: así esta llamada nunca "se va" a la API de OpenAI
        OpenAiChatOptions opciones = OpenAiChatOptions.builder()
                .baseUrl(propiedades.baseUrl())
                .apiKey(propiedades.apiKey())
                .model(propiedades.modelo())
                .temperature(0.1) // aún más bajo que el chat: aquí queremos extraer, no crear
                .maxTokens(MAX_TOKENS)
                .responseFormat(formato.build())
                .build();
        ChatResponse r = chat.call(new Prompt(List.of(new SystemMessage(sistema), new UserMessage(contenido)), opciones));
        String texto = r == null || r.getResult() == null || r.getResult().getOutput() == null
                ? null : r.getResult().getOutput().getText();
        if (texto == null || texto.isBlank()) {
            throw new IllegalStateException("Groq devolvió una respuesta vacía");
        }
        return texto;
    }

    // --- La FORMA del JSON. BeanOutputConverter lee estos records para generar el esquema.
    //     "required = true" en todos: el modo estricto de JSON Schema lo exige; los campos que no
    //     apliquen van como "" (por eso todo es texto, y el validador decide qué hacer con cada uno)

    record RespuestaIA(@JsonProperty(required = true, value = "propuestas") List<Item> propuestas) {
    }

    record Item(
            @JsonProperty(required = true, value = "tipo") String tipo,
            @JsonProperty(required = true, value = "titulo") String titulo,
            @JsonProperty(required = true, value = "descripcion") String descripcion,
            @JsonProperty(required = true, value = "fecha") String fecha,
            @JsonProperty(required = true, value = "horaInicio") String horaInicio,
            @JsonProperty(required = true, value = "horaFin") String horaFin,
            @JsonProperty(required = true, value = "sala") String sala,
            @JsonProperty(required = true, value = "tipoSesion") String tipoSesion,
            // TEXTO y no int: un int genera "format": "int32" en el esquema, y Groq lo rechaza (400:
            // "'format' is only allowed when type is 'string'"). Por eso el modo estricto NUNCA funcionó
            // y siempre caíamos al respaldo sin forma garantizada. Lo descubrimos leyendo el WARN del log.
            @JsonProperty(required = true, value = "fragmento") String fragmento,
            @JsonProperty(required = true, value = "cita") String cita) {

        PropuestaIA aPuerto() {
            return new PropuestaIA(tipo, titulo, descripcion, fecha, horaInicio, horaFin, sala, tipoSesion,
                    numero(fragmento), cita);
        }
    }

    /** "3", " 3 " o "[3]" -> 3. Cualquier otra cosa -> 0 ("no existe": el validador la descarta). */
    static int numero(String texto) {
        if (texto == null) {
            return 0;
        }
        String digitos = texto.replaceAll("[^0-9]", "");
        try {
            return digitos.isEmpty() ? 0 : Integer.parseInt(digitos);
        } catch (NumberFormatException e) {
            return 0; // un número absurdamente largo
        }
    }

    // --- La forma para LEER: igual, sin "required" y tolerante a campos de más que invente la IA

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaFlexible(List<ItemFlexible> propuestas) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ItemFlexible(String tipo, String titulo, String descripcion, String fecha, String horaInicio,
                        String horaFin, String sala, String tipoSesion, String fragmento, String cita) {

        PropuestaIA aPuerto() {
            return new PropuestaIA(tipo, titulo, descripcion, fecha, horaInicio, horaFin, sala, tipoSesion,
                    numero(fragmento), cita);
        }
    }
}
