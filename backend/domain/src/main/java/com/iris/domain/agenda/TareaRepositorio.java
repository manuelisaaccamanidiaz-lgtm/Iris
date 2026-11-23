package com.iris.domain.agenda;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida para tareas. Siempre DENTRO de un evento. */
public interface TareaRepositorio {

    Tarea guardar(Tarea tarea);

    /**
     * Tareas del evento: pendientes primero y por fecha límite.
     * @param estado        null = todas
     * @param responsableId null = de cualquier responsable
     */
    List<Tarea> listarPorEvento(UUID eventoId, EstadoTarea estado, UUID responsableId);

    Optional<Tarea> buscarEnEvento(UUID tareaId, UUID eventoId);

    void eliminar(UUID tareaId);

    /**
     * Para el despertador (4.3): tareas PENDIENTES con fecha límite <= hasta, de TODOS los eventos
     * que todavía no han terminado (fecha_fin >= hoy). Las de eventos pasados ya no se recuerdan.
     */
    List<Tarea> listarPendientesQueVencenHasta(LocalDate hasta, LocalDate hoy);
}
