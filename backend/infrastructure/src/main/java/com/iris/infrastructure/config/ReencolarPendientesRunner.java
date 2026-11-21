package com.iris.infrastructure.config;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.ColaIndexacion;
import com.iris.domain.documento.DocumentoRepositorio;

/**
 * Decisión 1A: si la app se apagó con documentos EN_COLA o PROCESANDO (hilos muertos a mitad
 * de camino), al arrancar los volvemos a poner en la fila. Funciona porque ProcesarDocumento es repetible.
 */
@Component
class ReencolarPendientesRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReencolarPendientesRunner.class);

    private final DocumentoRepositorio documentos;
    private final ColaIndexacion cola;

    ReencolarPendientesRunner(DocumentoRepositorio documentos, ColaIndexacion cola) {
        this.documentos = documentos;
        this.cola = cola;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<UUID> pendientes = documentos.idsPendientes();
        if (!pendientes.isEmpty()) {
            log.info("Re-encolando {} documento(s) pendiente(s)", pendientes.size());
            pendientes.forEach(cola::encolar);
        }
    }
}
