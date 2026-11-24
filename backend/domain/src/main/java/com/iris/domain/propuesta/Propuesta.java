package com.iris.domain.propuesta;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.iris.domain.agenda.Sesion;
import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TipoSesion;
import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.compartido.ReglaDeNegocioException;

/**
 * Algo que Iris PROPONE a partir de un documento: una tarea o una sesión, siempre con su FUENTE
 * (documento, páginas y la frase textual que la justifica). Nada entra a la agenda sin que una
 * persona lo acepte: Iris sugiere, el equipo decide.
 * Inmutable: ajustar, aceptar o descartar devuelven una Propuesta nueva.
 */
public record Propuesta(
        UUID id,
        UUID eventoId,
        UUID documentoId,
        String documentoNombre,
        TipoPropuesta tipo,
        EstadoPropuesta estado,
        String titulo,
        String descripcion,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String sala,
        TipoSesion tipoSesion,
        String cita,
        int paginaInicio,
        int paginaFin,
        Instant creadaEn,
        Instant decididaEn,
        UUID decididaPor,
        UUID resultadoId) {

    public static final int MAX_CITA = 500;

    public Propuesta {
        if (eventoId == null || tipo == null || estado == null || creadaEn == null) {
            throw new ReglaDeNegocioException("Faltan datos obligatorios de la propuesta");
        }
        titulo = texto(titulo, 200);
        if (titulo == null) {
            throw new ReglaDeNegocioException("La propuesta necesita un título");
        }
        descripcion = texto(descripcion, 5000);
        sala = texto(sala, 100);
        documentoNombre = texto(documentoNombre, 255);
        if (documentoNombre == null) {
            throw new ReglaDeNegocioException("La propuesta debe decir de qué documento salió");
        }
        cita = texto(cita, Integer.MAX_VALUE);
        if (cita == null) {
            throw new ReglaDeNegocioException("La propuesta debe citar su fuente");
        }
        if (cita.length() > MAX_CITA) {
            cita = cita.substring(0, MAX_CITA - 3) + "...";
        }
        if (paginaInicio < 1 || paginaFin < paginaInicio) {
            throw new ReglaDeNegocioException("Rango de páginas inválido");
        }
        // Mismas reglas que los CHECK de la V12 (doble protección)
        if (tipo == TipoPropuesta.SESION) {
            if (fecha == null || horaInicio == null || horaFin == null) {
                throw new ReglaDeNegocioException("Una sesión propuesta necesita fecha, hora de inicio y hora de fin");
            }
            if (!horaFin.isAfter(horaInicio)) {
                throw new ReglaDeNegocioException("La hora de fin debe ser posterior a la de inicio");
            }
        }
        if ((estado == EstadoPropuesta.PENDIENTE) != (decididaEn == null)) {
            throw new ReglaDeNegocioException("Una propuesta pendiente no tiene fecha de decisión (y una decidida sí)");
        }
        if ((estado == EstadoPropuesta.ACEPTADA) != (resultadoId != null)) {
            throw new ReglaDeNegocioException("Una propuesta aceptada debe apuntar a lo que se creó (y solo ella)");
        }
    }

    /** Los campos que una persona puede corregir antes de aceptar. null = "déjalo como lo propuso Iris". */
    public record Ajustes(String titulo, String descripcion, LocalDate fecha, LocalTime horaInicio,
                          LocalTime horaFin, String sala) {
    }

    public static Propuesta crear(UUID eventoId, UUID documentoId, String documentoNombre, TipoPropuesta tipo,
                                  String titulo, String descripcion, LocalDate fecha, LocalTime horaInicio,
                                  LocalTime horaFin, String sala, TipoSesion tipoSesion, String cita,
                                  int paginaInicio, int paginaFin) {
        return new Propuesta(null, eventoId, documentoId, documentoNombre, tipo, EstadoPropuesta.PENDIENTE,
                titulo, descripcion, fecha, horaInicio, horaFin, sala, tipoSesion, cita, paginaInicio, paginaFin,
                Instant.now(), null, null, null);
    }

    /** Aplica las correcciones de la persona. El constructor vuelve a validar todo (p. ej. las horas). */
    public Propuesta conAjustes(Ajustes a) {
        exigirPendiente();
        if (a == null) {
            return this;
        }
        return new Propuesta(id, eventoId, documentoId, documentoNombre, tipo, estado,
                a.titulo() != null ? a.titulo() : titulo,
                a.descripcion() != null ? a.descripcion() : descripcion,
                a.fecha() != null ? a.fecha() : fecha,
                a.horaInicio() != null ? a.horaInicio() : horaInicio,
                a.horaFin() != null ? a.horaFin() : horaFin,
                a.sala() != null ? a.sala() : sala,
                tipoSesion, cita, paginaInicio, paginaFin, creadaEn, null, null, null);
    }

    public Propuesta aceptar(UUID por, UUID resultadoId, Instant ahora) {
        exigirPendiente();
        return new Propuesta(id, eventoId, documentoId, documentoNombre, tipo, EstadoPropuesta.ACEPTADA,
                titulo, descripcion, fecha, horaInicio, horaFin, sala, tipoSesion, cita, paginaInicio, paginaFin,
                creadaEn, ahora, por, resultadoId);
    }

    public Propuesta descartar(UUID por, Instant ahora) {
        exigirPendiente();
        return new Propuesta(id, eventoId, documentoId, documentoNombre, tipo, EstadoPropuesta.DESCARTADA,
                titulo, descripcion, fecha, horaInicio, horaFin, sala, tipoSesion, cita, paginaInicio, paginaFin,
                creadaEn, ahora, por, null);
    }

    /** Lo que se le pasa a Tarea.crear al aceptar una propuesta de tipo TAREA. */
    public Tarea.Datos aDatosTarea(UUID responsableId) {
        return new Tarea.Datos(titulo, descripcion, fecha, responsableId);
    }

    /** Lo que se le pasa a Sesion.crear al aceptar una propuesta de tipo SESION. */
    public Sesion.Datos aDatosSesion() {
        return new Sesion.Datos(titulo, descripcion, fecha, horaInicio, horaFin, sala, null,
                tipoSesion != null ? tipoSesion : TipoSesion.OTRO);
    }

    private void exigirPendiente() {
        if (estado != EstadoPropuesta.PENDIENTE) {
            throw new ConflictoException("Esta propuesta ya fue " + estado.name().toLowerCase());
        }
    }

    /** null si viene vacío; recortado a "maximo" caracteres si es muy largo. */
    private static String texto(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String t = valor.trim();
        return t.length() > maximo ? t.substring(0, maximo) : t;
    }
}
