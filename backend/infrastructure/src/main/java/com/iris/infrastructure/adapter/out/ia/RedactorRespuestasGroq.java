package com.iris.infrastructure.adapter.out.ia;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.RedactorRespuestas;
import com.iris.infrastructure.config.ChatPropiedades;

/**
 * Adaptador: implementa RedactorRespuestas con Groq a través de la interfaz ChatModel de Spring AI.
 * Usamos ChatModel (y no ChatClient) a propósito: la búsqueda ya la hacemos nosotros, así que
 * solo necesitamos "enviar mensajes y recibir texto", sin capas extra que explicar.
 */
@Component
class RedactorRespuestasGroq implements RedactorRespuestas {

    private final ChatModel chat;
    private final ChatPropiedades propiedades;

    RedactorRespuestasGroq(ChatModel chat, ChatPropiedades propiedades) {
        this.chat = chat;
        this.propiedades = propiedades;
    }

    @Override
    public String redactar(String instrucciones, String mensaje) {
        ChatResponse respuesta = chat.call(prompt(instrucciones, mensaje));
        return texto(respuesta);
    }

    @Override
    public void redactarEnVivo(String instrucciones, String mensaje, Consumer<String> alRecibirTrozo) {
        // stream() devuelve un Flux (Reactor): una "manguera" de respuestas parciales.
        // Por cada trozo llamamos al Consumer, y blockLast() espera a que termine.
        chat.stream(prompt(instrucciones, mensaje))
                .map(RedactorRespuestasGroq::texto)
                .filter(t -> !t.isEmpty())
                .doOnNext(alRecibirTrozo)
                .blockLast();
    }

    @Override
    public String modelo() {
        return propiedades.modelo();
    }

    private static Prompt prompt(String instrucciones, String mensaje) {
        // SystemMessage = las reglas; UserMessage = contexto + pregunta
        return new Prompt(List.of(new SystemMessage(instrucciones), new UserMessage(mensaje)));
    }

    /** El texto de una respuesta (o de un trozo del streaming). Algunos trozos vienen vacíos. */
    private static String texto(ChatResponse r) {
        if (r == null || r.getResult() == null || r.getResult().getOutput() == null) {
            return "";
        }
        String t = r.getResult().getOutput().getText();
        return t == null ? "" : t;
    }
}
