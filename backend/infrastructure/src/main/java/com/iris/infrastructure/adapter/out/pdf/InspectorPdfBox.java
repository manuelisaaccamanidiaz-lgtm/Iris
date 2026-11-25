package com.iris.infrastructure.adapter.out.pdf;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.InspectorPdf;

/**
 * Adaptador: revisa la ESTRUCTURA del PDF con PDFBox. Un PDF por dentro es un árbol de diccionarios
 * ("objetos COS"). Recorremos TODO el árbol desde el catálogo (la raíz) buscando claves de contenido
 * ACTIVO. Así no importa dónde lo escondan: en la apertura del documento, en un botón, en una página...
 *
 * <p>Recorrido en PROFUNDIDAD con una pila y un conjunto de "ya visitados": los PDF tienen ciclos
 * (cada página apunta a su padre) y, sin ese conjunto, el recorrido no terminaría nunca.
 */
@Component
class InspectorPdfBox implements InspectorPdf {

    /** Tope de nodos: un PDF enorme o hecho a propósito no puede dejar el servidor recorriendo para siempre. */
    private static final int MAX_NODOS = 200_000;

    /** Claves que, si aparecen en CUALQUIER diccionario, delatan contenido activo. */
    private static final Map<String, String> CLAVES_PELIGROSAS = Map.of(
            "JS", "código JavaScript",
            "JavaScript", "código JavaScript",
            "EmbeddedFiles", "archivos escondidos dentro",
            "EF", "archivos escondidos dentro",
            "RichMedia", "contenido multimedia activo");

    /** Tipos de ACCIÓN (/S) que no deben estar en un documento de trabajo. */
    private static final Map<String, String> ACCIONES_PELIGROSAS = Map.of(
            "JavaScript", "código JavaScript",
            "Launch", "una acción que abre programas",
            "ImportData", "una acción que importa datos externos");

    @Override
    public List<String> riesgos(byte[] pdf) {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return List.copyOf(recorrer(documento.getDocumentCatalog().getCOSObject()));
        } catch (InvalidPasswordException e) {
            return List.of("una contraseña (así no se puede revisar ni leer)");
        } catch (IOException e) {
            return List.of("una estructura dañada que no se pudo revisar");
        }
    }

    static Set<String> recorrer(COSBase raiz) {
        Set<String> encontrados = new LinkedHashSet<>(); // sin repetidos y en el orden en que aparecen
        Deque<COSBase> pendientes = new ArrayDeque<>();
        Set<COSBase> vistos = Collections.newSetFromMap(new IdentityHashMap<>()); // por identidad, no por equals
        pendientes.push(raiz);
        int nodos = 0;
        while (!pendientes.isEmpty() && nodos++ < MAX_NODOS) {
            COSBase actual = pendientes.pop();
            if (actual instanceof COSObject referencia) {
                actual = referencia.getObject(); // una referencia "5 0 R": la seguimos hasta el objeto real
            }
            if (actual == null || !vistos.add(actual)) {
                continue;
            }
            if (actual instanceof COSDictionary diccionario) { // un COSStream también es un diccionario
                for (COSName clave : diccionario.keySet()) {
                    String riesgo = CLAVES_PELIGROSAS.get(clave.getName());
                    if (riesgo != null) {
                        encontrados.add(riesgo);
                    }
                    COSBase valor = diccionario.getItem(clave); // getItem: SIN resolver la referencia
                    if ("S".equals(clave.getName()) && valor instanceof COSName tipo) {
                        String accion = ACCIONES_PELIGROSAS.get(tipo.getName());
                        if (accion != null) {
                            encontrados.add(accion);
                        }
                    }
                    if (valor != null) {
                        pendientes.push(valor);
                    }
                }
            } else if (actual instanceof COSArray arreglo) {
                for (COSBase elemento : arreglo) {
                    if (elemento != null) {
                        pendientes.push(elemento);
                    }
                }
            }
        }
        return encontrados;
    }
}
