package com.iris.infrastructure.adapter.out.seguridad;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.iris.application.port.out.AntivirusNoDisponibleException;
import com.iris.infrastructure.config.AntivirusPropiedades;

/**
 * Sin ClamAV de verdad: un "ClamAV falso" en un ServerSocket que habla el MISMO protocolo.
 * Así probamos que enviamos bien los bytes (comando, trozos con su largo, el trozo 0 final)
 * y que entendemos las respuestas, sin necesitar Docker ni 4 GiB de RAM.
 */
class AntivirusClamAvTest {

    /** Arranca el falso: lee lo que manda Iris, lo guarda en "recibido" y contesta "respuesta". */
    private static int clamavFalso(String respuesta, AtomicReference<byte[]> recibido) throws IOException {
        ServerSocket servidor = new ServerSocket(0); // 0 = cualquier puerto libre
        Thread hilo = new Thread(() -> {
            try (servidor; Socket s = servidor.accept()) {
                DataInputStream entrada = new DataInputStream(s.getInputStream());
                byte[] comando = new byte["zINSTREAM\0".length()];
                entrada.readFully(comando);
                ByteArrayOutputStream archivo = new ByteArrayOutputStream();
                int largo;
                while ((largo = entrada.readInt()) > 0) { // trozos hasta el de largo 0
                    byte[] trozo = new byte[largo];
                    entrada.readFully(trozo);
                    archivo.write(trozo);
                }
                recibido.set(archivo.toByteArray());
                s.getOutputStream().write((respuesta + "\0").getBytes(StandardCharsets.US_ASCII));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        hilo.start();
        return servidor.getLocalPort();
    }

    private static AntivirusClamAv conPuerto(int puerto) {
        return new AntivirusClamAv(new AntivirusPropiedades(true, "localhost", puerto,
                Duration.ofSeconds(2), Duration.ofSeconds(5)));
    }

    @Test
    void unArchivoLimpioPasaYSeEnviaCompletoEnTrozos() throws IOException {
        AtomicReference<byte[]> recibido = new AtomicReference<>();
        byte[] grande = new byte[20_000]; // más de dos trozos de 8 KB
        for (int i = 0; i < grande.length; i++) {
            grande[i] = (byte) i;
        }
        Optional<String> r = conPuerto(clamavFalso("stream: OK", recibido)).escanear(grande);

        assertTrue(r.isEmpty());
        assertArrayEquals(grande, recibido.get()); // llegó byte por byte, en orden
    }

    @Test
    void unaAmenazaDevuelveSuNombre() throws IOException {
        Optional<String> r = conPuerto(clamavFalso("stream: Eicar-Signature FOUND", new AtomicReference<>()))
                .escanear("x".getBytes(StandardCharsets.US_ASCII));

        assertEquals(Optional.of("Eicar-Signature"), r);
    }

    @Test
    void siClamavNoRespondeNoSeAceptaElArchivo() throws IOException {
        int puertoCerrado;
        try (ServerSocket s = new ServerSocket(0)) {
            puertoCerrado = s.getLocalPort(); // se cierra al salir: nadie escucha ahí
        }
        AntivirusClamAv antivirus = conPuerto(puertoCerrado);

        assertThrows(AntivirusNoDisponibleException.class, () -> antivirus.escanear(new byte[] {1}));
    }

    @Test
    void apagadoNoEscaneaNiSeConecta() {
        AntivirusClamAv apagado = new AntivirusClamAv(new AntivirusPropiedades(false, "no-existe", 1,
                Duration.ofSeconds(1), Duration.ofSeconds(1)));

        assertTrue(apagado.escanear(new byte[] {1}).isEmpty());
    }

    @Test
    void unaRespuestaRaraSeTrataComoNoRevisado() {
        assertThrows(AntivirusNoDisponibleException.class,
                () -> AntivirusClamAv.interpretar("INSTREAM size limit exceeded. ERROR"));
    }
}
