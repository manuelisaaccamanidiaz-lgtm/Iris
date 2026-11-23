package com.iris.application.evento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.MiembroRepositorio;

/**
 * Caso de uso: me salgo del evento (decisiones 2A y 3c).
 * <ul>
 *   <li>Cualquier miembro: se borra su fila y pierde el acceso.</li>
 *   <li>El creador: solo si el evento ya terminó, y NO se borra: se archiva (trazabilidad).</li>
 * </ul>
 */
public class SalirDelEvento {

    private final AccesoEvento acceso;
    private final MiembroRepositorio miembros;

    public SalirDelEvento(AccesoEvento acceso, MiembroRepositorio miembros) {
        this.acceso = acceso;
        this.miembros = miembros;
    }

    public void ejecutar(UUID yo, UUID eventoId) {
        EventoDeMiembro evento = acceso.comoMiembro(eventoId, yo);
        evento.exigirPuedeSalir(yo, LocalDate.now()); // 400 si soy el creador y el evento no ha terminado

        if (evento.esCreador(yo)) {
            miembros.archivar(eventoId, yo, Instant.now());
        } else {
            miembros.quitar(eventoId, yo);
        }
    }
}
