package com.iris.application.consulta;

import java.util.List;

import com.iris.domain.consulta.Consulta;
import com.iris.domain.consulta.FuenteCitada;

/**
 * Quien "escucha" una consulta mientras ocurre. El caso de uso AVISA; el adaptador decide
 * cómo transmitirlo (en la web: eventos SSE). Java puro: application no sabe qué es SSE.
 */
public interface OyenteConsulta {

    /** Las fuentes numeradas que recibirá el modelo (llega ANTES del texto). */
    void fuentes(List<FuenteCitada> fuentes);

    /** Un pedacito de texto recién generado. */
    void trozo(String texto);

    /** La consulta terminó y quedó guardada (respuesta final con citas validadas). */
    void fin(Consulta consulta);

    /** Oyente que ignora todo: para la versión "sin streaming". */
    OyenteConsulta SILENCIOSO = new OyenteConsulta() {
        @Override public void fuentes(List<FuenteCitada> fuentes) { }
        @Override public void trozo(String texto) { }
        @Override public void fin(Consulta consulta) { }
    };
}
