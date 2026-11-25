package com.iris.application.port.out;

import java.util.List;

/**
 * Puerto de salida: la VALIDACIÓN de la estructura del PDF (la otra mitad del requisito).
 * Un antivirus busca amenazas CONOCIDAS; esto rechaza PDF con contenido ACTIVO aunque nadie lo haya
 * reportado todavía: JavaScript, acciones que abren programas, archivos escondidos dentro...
 * Un contrato o un cronograma legítimo no necesita nada de eso.
 */
public interface InspectorPdf {

    /** @return los riesgos encontrados en palabras de persona; lista vacía = el PDF está limpio. */
    List<String> riesgos(byte[] pdf);
}
