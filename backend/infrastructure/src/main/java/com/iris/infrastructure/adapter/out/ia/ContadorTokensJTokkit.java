package com.iris.infrastructure.adapter.out.ia;

import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.ContadorTokens;

/**
 * Adaptador: cuenta tokens con JTokkit (codificación cl100k_base), que ya viene con Spring AI.
 * No es el tokenizador exacto de Gemini, pero sí una aproximación muy buena para dimensionar fragmentos.
 */
@Component
class ContadorTokensJTokkit implements ContadorTokens {

    private final TokenCountEstimator estimador = new JTokkitTokenCountEstimator();

    @Override
    public int contar(String texto) {
        return texto == null || texto.isEmpty() ? 0 : estimador.estimate(texto);
    }
}
