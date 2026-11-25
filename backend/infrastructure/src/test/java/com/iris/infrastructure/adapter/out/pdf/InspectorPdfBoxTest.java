package com.iris.infrastructure.adapter.out.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.junit.jupiter.api.Test;

/** Creamos los PDF de prueba con el mismo PDFBox: uno limpio y uno con JavaScript al abrirse. */
class InspectorPdfBoxTest {

    private final InspectorPdfBox inspector = new InspectorPdfBox();

    private static byte[] guardar(PDDocument documento) throws IOException {
        try (documento; ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    @Test
    void unPdfNormalNoTieneRiesgos() throws IOException {
        PDDocument documento = new PDDocument();
        documento.addPage(new PDPage());

        assertTrue(inspector.riesgos(guardar(documento)).isEmpty());
    }

    @Test
    void unPdfQueEjecutaJavaScriptAlAbrirseSeDetecta() throws IOException {
        PDDocument documento = new PDDocument();
        documento.addPage(new PDPage());
        documento.getDocumentCatalog().setOpenAction(new PDActionJavaScript("app.alert('hola');"));

        assertEquals(List.of("código JavaScript"), inspector.riesgos(guardar(documento)));
    }
}
