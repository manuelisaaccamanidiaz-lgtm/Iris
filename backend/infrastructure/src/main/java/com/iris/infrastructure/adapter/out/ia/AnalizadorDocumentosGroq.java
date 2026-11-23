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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iris.application.port.out.AnalizadorDocumentos;
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
    private final BeanOutputConverter<RespuestaIA> conversor = new BeanOutputConverter<>(RespuestaIA.class);

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
            json = llamar(sistema, contenido, OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT);
        }
        RespuestaIA respuesta = conversor.convert(json);
        if (respuesta == null || respuesta.propuestas() == null) {
            return List.of();
        }
        return respuesta.propuestas().stream().map(Item::aPuerto).toList();
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
            @JsonProperty(required = true, value = "fragmento") int fragmento,
            @JsonProperty(required = true, value = "cita") String cita) {

        PropuestaIA aPuerto() {
            return new PropuestaIA(tipo, titulo, descripcion, fecha, horaInicio, horaFin, sala, tipoSesion,
                    fragmento, cita);
        }
    }
}
