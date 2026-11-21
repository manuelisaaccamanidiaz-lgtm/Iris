package com.iris.application.consulta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

class ConsultarIrisTest {

    @Test
    void borraLasCitasQueApuntanAFuentesInexistentes() {
        String texto = "Puedes cancelar hasta el 20 de abril [1] y pagar el saldo en mayo [7].";

        String limpio = ConsultarIris.limpiarCitasInvalidas(texto, 3);   // solo hay 3 fuentes

        assertEquals("Puedes cancelar hasta el 20 de abril [1] y pagar el saldo en mayo .", limpio);
    }

    @Test
    void numerosCitadosNoRepiteYOrdena() {
        String texto = "dato [2] otro [1] y otra vez [2]";

        assertEquals(Set.of(1, 2), ConsultarIris.numerosCitados(texto));
    }   
}