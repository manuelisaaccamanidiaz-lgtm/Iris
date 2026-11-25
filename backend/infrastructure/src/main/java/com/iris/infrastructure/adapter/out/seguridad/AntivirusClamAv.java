package com.iris.infrastructure.adapter.out.seguridad;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.Antivirus;
import com.iris.application.port.out.AntivirusNoDisponibleException;
import com.iris.infrastructure.config.AntivirusPropiedades;

/**
 * Adaptador: el puerto Antivirus con ClamAV, hablando su protocolo por TCP (puerto 3310). Sin librerías:
 * el protocolo es tan simple que se escribe en 30 líneas (documentación oficial, "clamd protocol"):
 * <pre>
 * "zINSTREAM\0"                         el comando (la z = termina en un byte nulo)
 * [4 bytes: largo][ese trozo del archivo]  ... repetido por trozos ...
 * [0 0 0 0]                             un trozo de largo 0 = "terminé"
 * respuesta: "stream: OK\0"  o  "stream: Eicar-Signature FOUND\0"
 * </pre>
 * Los 4 bytes del largo van en "orden de red" (big-endian): justo lo que escribe DataOutputStream.writeInt.
 */
@Component
class AntivirusClamAv implements Antivirus {

    private static final Logger log = LoggerFactory.getLogger(AntivirusClamAv.class);
    private static final int TROZO = 8 * 1024;

    private final AntivirusPropiedades propiedades;
    private final AtomicBoolean avisado = new AtomicBoolean(false);

    AntivirusClamAv(AntivirusPropiedades propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public Optional<String> escanear(byte[] contenido) {
        if (!propiedades.habilitado()) {
            // Se avisa UNA vez en el log (no en cada subida): apagarlo es una decisión, no un error
            if (avisado.compareAndSet(false, true)) {
                log.warn("Antivirus APAGADO (iris.antivirus.habilitado=false): los PDF no se escanean con ClamAV");
            }
            return Optional.empty();
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(propiedades.host(), propiedades.puerto()),
                    (int) propiedades.tiempoConexion().toMillis());
            socket.setSoTimeout((int) propiedades.tiempoRespuesta().toMillis()); // sin esto, esperaría para siempre
            DataOutputStream salida = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            salida.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            for (int desde = 0; desde < contenido.length; desde += TROZO) {
                int largo = Math.min(TROZO, contenido.length - desde);
                salida.writeInt(largo);
                salida.write(contenido, desde, largo);
            }
            salida.writeInt(0);
            salida.flush();
            return interpretar(leerHastaNulo(socket.getInputStream()));
        } catch (IOException e) {
            throw new AntivirusNoDisponibleException("ClamAV no respondió en " + propiedades.host() + ":"
                    + propiedades.puerto() + " (" + e.getMessage() + ")", e);
        }
    }

    /** "stream: OK" -> limpio; "stream: X FOUND" -> la amenaza X; cualquier otra cosa -> no pudimos revisar. */
    static Optional<String> interpretar(String respuesta) {
        String r = respuesta.strip();
        if (r.endsWith(" FOUND")) {
            return Optional.of(r.substring(r.indexOf(':') + 1, r.length() - " FOUND".length()).strip());
        }
        if (r.endsWith(": OK")) {
            return Optional.empty();
        }
        // p. ej. "INSTREAM size limit exceeded. ERROR": no sabemos si está limpio -> no lo aceptamos
        throw new AntivirusNoDisponibleException("Respuesta inesperada de ClamAV: " + r, null);
    }

    private static String leerHastaNulo(InputStream entrada) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int b;
        while ((b = entrada.read()) > 0) { // -1 = se cerró la conexión; 0 = fin de la respuesta (z)
            bytes.write(b);
        }
        return bytes.toString(StandardCharsets.US_ASCII);
    }
}
