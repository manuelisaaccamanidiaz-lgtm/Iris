package com.iris.application.propuesta;

import java.lang.System.Logger.Level;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.iris.application.notificacion.Avisos;
import com.iris.application.port.out.AnalizadorDocumentos;
import com.iris.application.port.out.ContadorTokens;
import com.iris.domain.agenda.Sesion;
import com.iris.domain.agenda.SesionRepositorio;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;
import com.iris.domain.documento.Documento;
import com.iris.domain.documento.DocumentoRepositorio;
import com.iris.domain.documento.EstadoDocumento;
import com.iris.domain.documento.FragmentoRepositorio;
import com.iris.domain.documento.TextoFragmento;
import com.iris.domain.evento.Evento;
import com.iris.domain.evento.EventoRepositorio;
import com.iris.domain.notificacion.TipoNotificacion;
import com.iris.domain.propuesta.EstadoPropuesta;
import com.iris.domain.propuesta.Propuesta;
import com.iris.domain.propuesta.PropuestaRepositorio;

/**
 * El corazón del 4.4: Iris LEE un documento indexado y PROPONE tareas y sesiones.
 *
 * <pre>
 * fragmentos (en orden, con tope) -> prompt -> IA (JSON Schema) -> VALIDAR -> reemplazar pendientes -> avisar
 * </pre>
 *
 * Lo usan dos disparadores (decisión de Isaac: "ambos"):
 * <ul>
 *   <li>automático: la cola de indexación, al terminar de indexar ({@link #paraDocumentoIndexado});</li>
 *   <li>bajo pedido: "revisar este documento" ({@link RevisarDocumento}).</li>
 * </ul>
 */
public class GenerarPropuestas {

    private static final System.Logger LOG = System.getLogger(GenerarPropuestas.class.getName());

    private final FragmentoRepositorio fragmentos;
    private final ContadorTokens contador;
    private final AnalizadorDocumentos analizador;
    private final PropuestaRepositorio propuestas;
    private final DocumentoRepositorio documentos;
    private final EventoRepositorio eventos;
    private final Avisos avisos;
    private final TareaRepositorio tareas;
    private final SesionRepositorio sesiones;

    public GenerarPropuestas(FragmentoRepositorio fragmentos, ContadorTokens contador,
                             AnalizadorDocumentos analizador, PropuestaRepositorio propuestas,
                             DocumentoRepositorio documentos, EventoRepositorio eventos, Avisos avisos,
                             TareaRepositorio tareas, SesionRepositorio sesiones) {
        this.fragmentos = fragmentos;
        this.contador = contador;
        this.analizador = analizador;
        this.propuestas = propuestas;
        this.documentos = documentos;
        this.eventos = eventos;
        this.avisos = avisos;
        this.tareas = tareas;
        this.sesiones = sesiones;
    }

    /** El núcleo: quien llama ya comprobó permisos y que el documento esté INDEXADO. */
    public List<Propuesta> ejecutar(Documento doc, Evento evento) {
        List<TextoFragmento> texto = PromptPropuestas.seleccionar(fragmentos.listarTextosDelDocumento(doc.id()), contador);
        if (texto.isEmpty()) {
            return List.of();
        }
        // v2: lo que el evento YA tiene, para que Iris busque lo que FALTA (y no repita)
        LocalDate hoy = LocalDate.now();
        List<String> yaExisten = new ArrayList<>();
        tareas.listarPorEvento(evento.id(), null, null).stream().map(Tarea::titulo).forEach(yaExisten::add);
        sesiones.listarPorEvento(evento.id()).stream().map(Sesion::titulo).forEach(yaExisten::add);
        List<String> descartadas = new ArrayList<>();
        for (Propuesta p : propuestas.listarPorEvento(evento.id(), null)) {
            if (p.estado() == EstadoPropuesta.DESCARTADA) {
                descartadas.add(p.titulo());
            } else if (p.estado() == EstadoPropuesta.ACEPTADA || !Objects.equals(p.documentoId(), doc.id())) {
                yaExisten.add(p.titulo()); // aceptadas, y pendientes de OTROS documentos (las de este se reemplazan)
            }
        }

        String contenido = PromptPropuestas.contenido(evento, doc.nombreArchivo(), texto, hoy, yaExisten, descartadas);
        List<AnalizadorDocumentos.PropuestaIA> brutas = analizador.proponer(PromptPropuestas.INSTRUCCIONES, contenido);

        ValidadorPropuestas.Resultado r = ValidadorPropuestas.validar(brutas, texto, doc.id(), doc.nombreArchivo(), evento,
                ValidadorPropuestas.Contexto.de(hoy, yaExisten, descartadas));
        if (r.descartadas() > 0) {
            LOG.log(Level.INFO, "Propuestas descartadas por el validador en «" + doc.nombreArchivo() + "»: " + r.descartadas());
        }
        List<Propuesta> guardadas = propuestas.reemplazarPendientesDelDocumento(doc.id(), r.validas());

        if (!guardadas.isEmpty()) {
            String mensaje = "Iris leyó «" + doc.nombreArchivo() + "» y propone " + guardadas.size()
                    + (guardadas.size() == 1 ? " acción" : " acciones")
                    + " (tareas o sesiones). Revísalas antes de que entren a la agenda";
            UUID primera = guardadas.get(0).id();
            if (doc.subidoPor() != null) {
                avisos.aMiembro(doc.eventoId(), doc.subidoPor(), TipoNotificacion.PROPUESTAS_NUEVAS, mensaje, primera);
            }
            avisos.aPropietarios(doc.eventoId(), TipoNotificacion.PROPUESTAS_NUEVAS, mensaje, primera, doc.subidoPor());
        }
        return guardadas;
    }

    /**
     * Disparador AUTOMÁTICO (sin usuario delante). Busca el evento "como" quien subió el documento:
     * así se respeta la regla de siempre (no existe "buscar evento por id" a secas). Si esa persona
     * ya no es miembro o borró su cuenta, no se generan propuestas automáticas (siguen las de pedido).
     */
    public void paraDocumentoIndexado(UUID documentoId) {
        documentos.buscarParaProcesar(documentoId)
                .filter(d -> d.estado() == EstadoDocumento.INDEXADO && d.subidoPor() != null)
                .ifPresent(d -> eventos.buscarComoMiembro(d.eventoId(), d.subidoPor())
                        .ifPresent(e -> ejecutar(d, e.evento())));
    }
}
