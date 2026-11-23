package com.iris.application.notificacion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import com.iris.domain.agenda.Tarea;
import com.iris.domain.agenda.TareaRepositorio;
import com.iris.domain.notificacion.RecordatorioRepositorio;
import com.iris.domain.notificacion.TipoNotificacion;

/**
 * Caso de uso del DESPERTADOR (decisión 1B): recorre las tareas PENDIENTES que vencen
 * hasta mañana y manda a cada una, como máximo una vez, "vence mañana", "vence hoy" o "vencida".
 * Si la tarea no tiene responsable, el aviso va a los propietarios para que la asignen.
 */
public class EnviarRecordatorios {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.of("es"));

    private final TareaRepositorio tareas;
    private final RecordatorioRepositorio recordatorios;
    private final Avisos avisos;

    public EnviarRecordatorios(TareaRepositorio tareas, RecordatorioRepositorio recordatorios, Avisos avisos) {
        this.tareas = tareas;
        this.recordatorios = recordatorios;
        this.avisos = avisos;
    }

    /** @return cuántos recordatorios NUEVOS se enviaron (los repetidos no cuentan) */
    public int ejecutar(LocalDate hoy) {
        int enviados = 0;
        for (Tarea t : tareas.listarPendientesQueVencenHasta(hoy.plusDays(1))) {
            Optional<TipoNotificacion> tipo = TipoNotificacion.recordatorioPara(t.fechaLimite(), hoy);
            // registrarSiEsNuevo es la "memoria": si ya se envió, la PK de la V11 lo frena
            if (tipo.isEmpty() || !recordatorios.registrarSiEsNuevo(t.id(), tipo.get(), t.fechaLimite())) {
                continue;
            }
            String mensaje = mensaje(t, tipo.get());
            if (t.responsableId() != null) {
                avisos.aMiembro(t.eventoId(), t.responsableId(), tipo.get(), mensaje, t.id());
            } else {
                avisos.aPropietarios(t.eventoId(), tipo.get(), mensaje + " (no tiene responsable)", t.id());
            }
            enviados++;
        }
        return enviados;
    }

    private static String mensaje(Tarea t, TipoNotificacion tipo) {
        String fecha = t.fechaLimite().format(FECHA);
        return switch (tipo) {
            case TAREA_VENCE_MANANA -> "La tarea «" + t.titulo() + "» vence mañana (" + fecha + ")";
            case TAREA_VENCE_HOY -> "La tarea «" + t.titulo() + "» vence HOY (" + fecha + ")";
            case TAREA_VENCIDA -> "La tarea «" + t.titulo() + "» está vencida desde el " + fecha;
            default -> throw new IllegalArgumentException("No es un tipo de recordatorio: " + tipo);
        };
    }
}
