package com.iris.infrastructure.adapter.out.cola;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.iris.application.documento.ProcesarDocumento;
import com.iris.application.port.out.ColaIndexacion;

/**
 * Adaptador: implementa la cola con @Async. Spring envuelve este bean en un PROXY:
 * cuando alguien llama encolar(), el proxy pasa el trabajo al pool "indexadorExecutor"
 * y regresa de inmediato. Por eso funciona solo si lo llama OTRO bean (no "this.encolar()").
 */
@Component
class ColaIndexacionAsync implements ColaIndexacion {

    private static final Logger log = LoggerFactory.getLogger(ColaIndexacionAsync.class);

    private final ProcesarDocumento procesarDocumento;

    ColaIndexacionAsync(ProcesarDocumento procesarDocumento) {
        this.procesarDocumento = procesarDocumento;
    }

    @Override
    @Async("indexadorExecutor")
    public void encolar(UUID documentoId) {
        log.info("Indexando documento {}", documentoId); // mira el nombre del hilo en la consola
        try {
            procesarDocumento.ejecutar(documentoId);
            log.info("Documento {} procesado", documentoId);
        } catch (Exception e) {
            // Última red: algo falló incluso guardando el ERROR (p. ej. la BD cayó). Solo queda el log
            log.error("Fallo inesperado indexando {}", documentoId, e);
        }
    }
}
