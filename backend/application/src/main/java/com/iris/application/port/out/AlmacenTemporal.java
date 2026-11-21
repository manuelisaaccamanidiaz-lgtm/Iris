package com.iris.application.port.out;

/**
 * Puerto de salida: dónde guardar la copia temporal del PDF mientras se indexa.
 * Hoy es una carpeta del disco; mañana podría ser otro almacenamiento sin tocar los casos de uso.
 */
public interface AlmacenTemporal {

    /** Guarda los bytes y devuelve la ruta donde quedaron. */
    String guardar(byte[] contenido);

    /** Lee la copia para procesarla. */
    byte[] leer(String ruta);

    /** Borra la copia. No falla si ya no existe. */
    void borrar(String ruta);
}
