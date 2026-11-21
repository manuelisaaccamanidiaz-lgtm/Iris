package com.iris.infrastructure.adapter.out.almacenamiento;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.AlmacenTemporal;
import com.iris.infrastructure.config.DocumentosPropiedades;

/**
 * Adaptador: guarda las copias temporales en una carpeta del disco.
 * El nombre del archivo es un UUID al azar, NUNCA el nombre que mandó el usuario
 * (así nadie puede escribir fuera de la carpeta con nombres tipo "../../algo.pdf").
 */
@Component
class AlmacenTemporalDisco implements AlmacenTemporal {

    private static final Logger log = LoggerFactory.getLogger(AlmacenTemporalDisco.class);

    private final Path directorio;

    AlmacenTemporalDisco(DocumentosPropiedades propiedades) {
        this.directorio = Path.of(propiedades.directorioTemporal()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directorio); // la crea si no existe
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta temporal " + directorio, e);
        }
        log.info("Copias temporales de PDF en: {}", directorio);
    }

    @Override
    public String guardar(byte[] contenido) {
        Path destino = directorio.resolve(UUID.randomUUID() + ".pdf");
        try {
            Files.write(destino, contenido);
            return destino.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la copia temporal", e);
        }
    }

    @Override
    public byte[] leer(String ruta) {
        try {
            return Files.readAllBytes(Path.of(ruta));
        } catch (IOException e) {
            throw new UncheckedIOException("No se encontró la copia temporal del PDF", e);
        }
    }

    @Override
    public void borrar(String ruta) {
        if (ruta == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(ruta));
        } catch (IOException e) {
            // No interrumpimos el flujo por esto, pero lo dejamos registrado
            log.warn("No se pudo borrar la copia temporal {}", ruta, e);
        }
    }
}
