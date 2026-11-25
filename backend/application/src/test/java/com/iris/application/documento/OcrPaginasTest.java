package com.iris.application.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.iris.application.port.out.ExtractorTexto.PaginaTexto;
import com.iris.application.port.out.LectorOcr;

/** La regla "OCR cuando sea necesario", con un OCR FALSO que anota qué páginas le pidieron. */
class OcrPaginasTest {

    private final List<Integer> pedidas = new ArrayList<>();
    private final LectorOcr ocrFalso = (pdf, numeros) -> {
        pedidas.addAll(numeros);
        return numeros.stream().map(n -> "Texto leído por OCR de la página " + n).toList();
    };

    private static PaginaTexto pagina(int n, String texto) {
        return new PaginaTexto(n, texto);
    }

    @Test
    void unPdfDigitalNoLlamaAlOcr() {
        List<PaginaTexto> r = ProcesarDocumento.completarConOcr(new byte[0],
                List.of(pagina(1, "CLÁUSULA 1. El hotel reserva el salón principal.")), ocrFalso);

        assertTrue(pedidas.isEmpty()); // ni una llamada a Gemini: no gastamos cuota
        assertEquals("CLÁUSULA 1. El hotel reserva el salón principal.", r.get(0).texto());
    }

    @Test
    void soloLasPaginasEscaneadasVanAlOcrYConservanSuNumero() {
        List<PaginaTexto> r = ProcesarDocumento.completarConOcr(new byte[0], List.of(
                pagina(1, "CLÁUSULA 1. El hotel reserva el salón principal."),
                pagina(2, "   "),          // escaneada: sin texto
                pagina(3, "Firma")),       // casi sin texto: un anexo escaneado con una palabra suelta
                ocrFalso);

        assertEquals(List.of(2, 3), pedidas);
        assertEquals(3, r.size());
        assertEquals("Texto leído por OCR de la página 2", r.get(1).texto());
        assertEquals(2, r.get(1).numero()); // la cita seguirá diciendo "pág. 2"
    }

    @Test
    void demasiadasPaginasEscaneadasSeRechazanAntesDeLlamarAlOcr() {
        List<PaginaTexto> muchas = IntStream.rangeClosed(1, ProcesarDocumento.MAX_PAGINAS_OCR + 1)
                .mapToObj(n -> pagina(n, "")).toList();

        assertThrows(IllegalStateException.class, () -> ProcesarDocumento.completarConOcr(new byte[0], muchas, ocrFalso));
        assertTrue(pedidas.isEmpty());
    }
}
