package com.iris.application.port.out;

import java.util.Optional;

/**
 * Puerto de salida: el ESCANEO de archivos del requisito ("límites de tamaño, validación y escaneo").
 * Hoy lo implementa ClamAV (antivirus de código abierto) corriendo en Docker.
 */
public interface Antivirus {

    /**
     * @return vacío si el archivo está limpio, o el nombre de la amenaza encontrada (p. ej. "Eicar-Signature")
     * @throws AntivirusNoDisponibleException si el antivirus está encendido en la configuración pero no responde
     */
    Optional<String> escanear(byte[] contenido);
}
