package com.iris.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bloque "iris.antivirus": dónde está ClamAV y cuánto esperarlo.
 * @param habilitado encendido por defecto. Se puede apagar (ANTIVIRUS_HABILITADO=false) en un equipo sin
 *                   RAM suficiente: ClamAV pide de 3 a 4 GiB (documentación oficial)
 */
@ConfigurationProperties(prefix = "iris.antivirus")
public record AntivirusPropiedades(boolean habilitado, String host, int puerto,
                                   Duration tiempoConexion, Duration tiempoRespuesta) {
}
