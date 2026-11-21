package com.iris.infrastructure.adapter.out.pdf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.ExtractorTexto;

/** Adaptador: implementa ExtractorTexto con Apache PDFBox 3. */
@Component
class ExtractorPdfBox implements ExtractorTexto {

    @Override
    public List<PaginaTexto> extraer(byte[] pdf) {
        // try-with-resources: el PDDocument se cierra solo al terminar (libera memoria)
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // respeta el orden visual (útil en columnas)

            int total = documento.getNumberOfPages();
            List<PaginaTexto> paginas = new ArrayList<>(total);
            // Extraemos UNA página a la vez para saber exactamente de qué página sale cada texto
            for (int numero = 1; numero <= total; numero++) {
                stripper.setStartPage(numero);
                stripper.setEndPage(numero);
                paginas.add(new PaginaTexto(numero, stripper.getText(documento).strip()));
            }
            return paginas;
        } catch (InvalidPasswordException e) {
            throw new IllegalStateException("El PDF está protegido con contraseña", e);
        } catch (IOException e) {
            throw new UncheckedIOException("El PDF está dañado o no se pudo leer", e);
        }
    }
}
