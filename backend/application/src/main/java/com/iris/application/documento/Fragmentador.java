package com.iris.application.documento;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.iris.application.port.out.ContadorTokens;
import com.iris.application.port.out.ExtractorTexto.PaginaTexto;

/**
 * Parte el texto de un documento en fragmentos de 500-1000 tokens con solapamiento,
 * SIN perder de qué página(s) y sección sale cada uno (requisito del profe: paso 4).
 * Es Java puro: se puede probar con un ContadorTokens falso (ver el mini ejercicio).
 */
public class Fragmentador {

    static final int META_TOKENS = 800;     // al llegar aquí, cerramos el fragmento
    static final int MAXIMO_TOKENS = 1000;  // nunca lo superamos
    static final int SOLAPE_TOKENS = 120;   // ~15 %: el "bordecito" que se repite
    static final int MINIMO_ULTIMO = 250;   // un último fragmento muy pequeño se une al anterior

    // Líneas que parecen títulos de sección en documentos de eventos
    private static final Pattern TITULO = Pattern.compile(
            "^(cl[aá]usula|art[ií]culo|cap[ií]tulo|secci[oó]n|anexo|t[ií]tulo)\\b.*",
            Pattern.CASE_INSENSITIVE);

    private final ContadorTokens contador;

    public Fragmentador(ContadorTokens contador) {
        this.contador = contador;
    }

    /** El resultado: un fragmento SIN embedding todavía (eso lo agrega ProcesarDocumento). */
    public record Pedazo(int paginaInicio, int paginaFin, String seccion, String contenido, int tokens) {
    }

    /** Una línea con su página, su sección vigente y sus tokens. */
    private record Linea(int pagina, String seccion, String texto, int tokens) {
    }

    public List<Pedazo> fragmentar(List<PaginaTexto> paginas) {
        List<Linea> lineas = aLineas(paginas);
        List<List<Linea>> grupos = new ArrayList<>();
        List<Linea> actual = new ArrayList<>();
        int tokensActual = 0;
        boolean hayNuevo = false; // ¿el grupo actual tiene algo más que el solapamiento?

        for (Linea linea : lineas) {
            if (hayNuevo && tokensActual + linea.tokens() > MAXIMO_TOKENS) {
                grupos.add(actual);
                actual = solapamiento(actual);
                tokensActual = suma(actual);
                hayNuevo = false;
            }
            actual.add(linea);
            tokensActual += linea.tokens();
            hayNuevo = true;
            if (tokensActual >= META_TOKENS) {
                grupos.add(actual);
                actual = solapamiento(actual);
                tokensActual = suma(actual);
                hayNuevo = false;
            }
        }
        if (hayNuevo) {
            // Último grupo: si es muy pequeño y cabe, lo unimos al anterior
            if (!grupos.isEmpty() && tokensActual < MINIMO_ULTIMO) {
                List<Linea> anterior = grupos.get(grupos.size() - 1);
                List<Linea> soloNuevas = actual.subList(cantidadSolapadas(anterior, actual), actual.size());
                if (suma(anterior) + suma(soloNuevas) <= MAXIMO_TOKENS) {
                    anterior.addAll(soloNuevas);
                } else {
                    grupos.add(actual);
                }
            } else {
                grupos.add(actual);
            }
        }
        return grupos.stream().map(this::aPedazo).toList();
    }

    /** Parte cada página en líneas no vacías y recuerda la sección vigente. */
    private List<Linea> aLineas(List<PaginaTexto> paginas) {
        List<Linea> lineas = new ArrayList<>();
        String seccion = null;
        for (PaginaTexto pagina : paginas) {
            for (String cruda : pagina.texto().split("\\R")) { // \R = cualquier salto de línea
                String texto = cruda.strip();
                if (texto.isEmpty()) {
                    continue;
                }
                if (TITULO.matcher(texto).matches()) {
                    seccion = tituloCorto(texto);
                }
                for (String trozo : partirSiEsMuyLarga(texto)) {
                    lineas.add(new Linea(pagina.numero(), seccion, trozo, Math.max(1, contador.contar(trozo))));
                }
            }
        }
        return lineas;
    }

    /** "Cláusula 3 - Cancelación. El cliente podrá..." -> "Cláusula 3 - Cancelación" */
    private static String tituloCorto(String linea) {
        int punto = linea.indexOf('.');
        String titulo = punto > 0 ? linea.substring(0, punto) : linea;
        return titulo.length() > 120 ? titulo.substring(0, 120) : titulo;
    }

    /** Caso raro: una sola "línea" gigante (PDF sin saltos). La partimos por palabras. */
    private List<String> partirSiEsMuyLarga(String texto) {
        if (contador.contar(texto) <= MAXIMO_TOKENS / 2) {
            return List.of(texto);
        }
        List<String> trozos = new ArrayList<>();
        StringBuilder trozo = new StringBuilder();
        for (String palabra : texto.split("\\s+")) {
            if (trozo.length() > 0 && contador.contar(trozo + " " + palabra) > MAXIMO_TOKENS / 2) {
                trozos.add(trozo.toString());
                trozo.setLength(0);
            }
            if (trozo.length() > 0) {
                trozo.append(' ');
            }
            trozo.append(palabra);
        }
        if (trozo.length() > 0) {
            trozos.add(trozo.toString());
        }
        return trozos;
    }

    /** Las últimas líneas del grupo que sumen hasta ~SOLAPE_TOKENS: con ellas empieza el siguiente. */
    private static List<Linea> solapamiento(List<Linea> grupo) {
        List<Linea> cola = new ArrayList<>();
        int tokens = 0;
        for (int i = grupo.size() - 1; i > 0; i--) { // i > 0: nunca copiamos el grupo entero
            Linea l = grupo.get(i);
            if (tokens + l.tokens() > SOLAPE_TOKENS) {
                break;
            }
            cola.add(0, l);
            tokens += l.tokens();
        }
        return cola;
    }

    /** Cuántas líneas iniciales de "actual" son el solapamiento copiado de "anterior". */
    private static int cantidadSolapadas(List<Linea> anterior, List<Linea> actual) {
        return Math.min(solapamiento(anterior).size(), actual.size());
    }

    private static int suma(List<Linea> lineas) {
        return lineas.stream().mapToInt(Linea::tokens).sum();
    }

    private Pedazo aPedazo(List<Linea> grupo) {
        String contenido = String.join("\n", grupo.stream().map(Linea::texto).toList());
        int inicio = grupo.stream().mapToInt(Linea::pagina).min().orElse(1);
        int fin = grupo.stream().mapToInt(Linea::pagina).max().orElse(inicio);
        // La sección del fragmento: la vigente en su primera línea (o la primera que aparezca)
        String seccion = grupo.stream().map(Linea::seccion).filter(s -> s != null).findFirst().orElse(null);
        return new Pedazo(inicio, fin, seccion, contenido, contador.contar(contenido));
    }
}
