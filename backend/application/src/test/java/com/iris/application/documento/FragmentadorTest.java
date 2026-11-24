package com.iris.application.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.iris.application.port.out.ExtractorTexto.PaginaTexto;

class FragmentadorTest {

    // Contador FALSO: 1 token por palabra. ContadorTokens tiene un solo método, así que basta una lambda
    private final Fragmentador fragmentador =
            new Fragmentador(texto -> texto.isBlank() ? 0 : texto.trim().split("\\s+").length);

    @Test
    void textoCortoDaUnSoloFragmentoConSusPaginasYSeccion() {
        var paginas = List.of(
                new PaginaTexto(1, "Cláusula 1 - Objeto. Alquiler del salón"),
                new PaginaTexto(2, "para trescientas personas"));

        var resultado = fragmentador.fragmentar(paginas);

        assertEquals(1, resultado.size());
        assertEquals(1, resultado.get(0).paginaInicio());
        assertEquals(2, resultado.get(0).paginaFin());
        assertEquals("Cláusula 1 - Objeto", resultado.get(0).seccion());
    }

    @Test
    void textoLargoSeParteSinSuperarMilTokens() {
        // Arrange: una línea de 10 palabras = 10 tokens con nuestro contador falso
        String linea = "uno dos tres cuatro cinco seis siete ocho nueve diez\n";
        // .repeat(100) = 100 líneas por página = 1000 tokens por página -> 3000 en total
        String textoPagina = linea.repeat(100);
        var paginas = List.of(
                new PaginaTexto(1, textoPagina),
                new PaginaTexto(2, textoPagina),
                new PaginaTexto(3, textoPagina));

        // Act
        var resultado = fragmentador.fragmentar(paginas);

        // Assert 1: 3000 tokens NO caben en un fragmento de máximo 1000 -> debe haber varios
        assertTrue(resultado.size() > 1, "Debería partirse en varios fragmentos");

        // Assert 2: NINGUNO supera el máximo (recorremos todos con un for)
        for (var pedazo : resultado) {
            assertTrue(pedazo.tokens() <= 1000, "Un fragmento tiene " + pedazo.tokens() + " tokens");
        }

        // Assert 3: el primero empieza en la página 1 y el último termina en la 3 (no se perdió nada)
        assertEquals(1, resultado.get(0).paginaInicio());
        assertEquals(3, resultado.get(resultado.size() - 1).paginaFin());
    }

    @Test
    void elSegundoFragmentoRepiteElFinalDelPrimero() {
        // Líneas numeradas para poder reconocer cuál se repite
        StringBuilder texto = new StringBuilder();
        for (int i = 1; i <= 200; i++) {
            texto.append("linea ").append(i).append(" con ocho palabras de relleno aqui\n");
        }
        var resultado = fragmentador.fragmentar(List.of(new PaginaTexto(1, texto.toString())));

        // La última línea del fragmento 1 debe aparecer al INICIO del fragmento 2: eso es el solapamiento
        String[] lineasPrimero = resultado.get(0).contenido().split("\n");
        String ultimaDelPrimero = lineasPrimero[lineasPrimero.length - 1];
        assertTrue(resultado.get(1).contenido().contains(ultimaDelPrimero),
                "El segundo fragmento debería repetir: " + ultimaDelPrimero);
    }
}
