package com.iris.application.port.out;

/**
 * El antivirus debía revisar el archivo y no respondió. Decisión de seguridad: "fallar CERRADO".
 * Si no pudimos revisarlo, NO lo aceptamos (la web responde 503: "intenta en un momento").
 * Lo contrario, aceptar sin revisar cuando el antivirus se cae, sería un agujero que un atacante
 * podría provocar a propósito.
 */
public class AntivirusNoDisponibleException extends RuntimeException {

    public AntivirusNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
