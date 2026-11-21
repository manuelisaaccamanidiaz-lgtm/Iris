package com.iris.application.port.out;

import java.util.List;

/** Puerto de salida: saca el texto de un PDF, página por página (hoy lo implementa PDFBox). */
public interface ExtractorTexto {

    List<PaginaTexto> extraer(byte[] pdf);

    /** El texto de UNA página, con su número (empieza en 1). La página es la base de las citas. */
    record PaginaTexto(int numero, String texto) {
    }
}
