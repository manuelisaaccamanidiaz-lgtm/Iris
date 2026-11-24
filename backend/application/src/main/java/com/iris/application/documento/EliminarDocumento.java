package com.iris.application.documento;

import java.util.List;
import java.util.UUID;

import com.iris.application.evento.AccesoEvento;
import com.iris.application.port.out.AlmacenTemporal;
import com.iris.application.port.out.Transaccion;
import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.documento.Documento;
import com.iris.domain.documento.DocumentoRepositorio;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.propuesta.PropuestaRepositorio;

/**
 * Caso de uso (Día 4.5): borrar un documento DE VERDAD (borrado físico): su registro, sus
 * fragmentos y sus vectores. Es lo que espera quien sube algo por error.
 *
 * <p>Qué pasa con lo que dependía de él (decisiones de Isaac):
 * <ul>
 *   <li>fragmentos: se borran (CASCADE de la V6);</li>
 *   <li>propuestas PENDIENTES: se borran (ya no tienen sentido);</li>
 *   <li>propuestas aceptadas o descartadas: se quedan como historial (SET NULL de la V12 + nombre copiado);</li>
 *   <li>citas del historial de consultas: se quedan (SET NULL de la V8 + nombre y páginas copiados);</li>
 *   <li>tareas y sesiones creadas desde sus propuestas: se quedan (ya son trabajo del equipo).</li>
 * </ul>
 */
public class EliminarDocumento {

    private final AccesoEvento acceso;
    private final DocumentoRepositorio documentos;
    private final PropuestaRepositorio propuestas;
    private final AlmacenTemporal almacen;
    private final Transaccion transaccion;

    public EliminarDocumento(AccesoEvento acceso, DocumentoRepositorio documentos, PropuestaRepositorio propuestas,
                             AlmacenTemporal almacen, Transaccion transaccion) {
        this.acceso = acceso;
        this.documentos = documentos;
        this.propuestas = propuestas;
        this.almacen = almacen;
        this.transaccion = transaccion;
    }

    public void ejecutar(UUID yo, UUID eventoId, UUID documentoId) {
        // Guardas en orden: miembro (404) -> editor (403) -> existe en ESTE evento (404) -> autor o propietario (403)
        // -> no se está indexando (409)
        EventoDeMiembro evento = acceso.comoEditor(eventoId, yo);
        Documento doc = documentos.buscarParaProcesar(documentoId)
                .filter(d -> d.eventoId().equals(eventoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));
        doc.exigirQueSePuedaBorrar(yo, evento.miRol());

        transaccion.ejecutarSinResultado(() -> {
            // Reutilizamos el 4.4: "reemplazar las pendientes por NADA" = borrarlas
            propuestas.reemplazarPendientesDelDocumento(doc.id(), List.of());
            if (!documentos.eliminarSiNoSeEstaIndexando(doc.id())) {
                // Empezó a indexarse justo ahora: el ROLLBACK devuelve también las propuestas pendientes
                throw new ConflictoException("El documento empezó a indexarse; espera a que termine para borrarlo");
            }
        });

        // Después del COMMIT: la copia temporal del PDF (si quedaba, p. ej. en EN_COLA o ERROR).
        // Si un documento EN_COLA se borra, el indexador ya no lo encuentra y simplemente lo salta
        almacen.borrar(doc.rutaTemporal());
    }
}
