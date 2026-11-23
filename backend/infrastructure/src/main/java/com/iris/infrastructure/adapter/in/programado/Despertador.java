package com.iris.infrastructure.adapter.in.programado;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.iris.application.notificacion.EnviarRecordatorios;
import com.iris.application.notificacion.VencerInvitaciones;

/**
 * El DESPERTADOR: un adaptador de ENTRADA, como un controlador, pero quien "llama" no es
 * una persona con HTTP sino el reloj. Por eso vive en adapter/in y solo delega en casos de uso.
 */
@Component
public class Despertador {

    private static final Logger log = LoggerFactory.getLogger(Despertador.class);

    private final EnviarRecordatorios recordatorios;
    private final VencerInvitaciones invitaciones;
    private final ZoneId zona;

    Despertador(EnviarRecordatorios recordatorios, VencerInvitaciones invitaciones,
                @Value("${iris.recordatorios.zona}") String zona) {
        this.recordatorios = recordatorios;
        this.invitaciones = invitaciones;
        this.zona = ZoneId.of(zona);
    }

    /** Todos los días a las 6:00 a. m. de Bogotá (cron y zona vienen del application.yml). */
    @Scheduled(cron = "${iris.recordatorios.cron}", zone = "${iris.recordatorios.zona}")
    void cadaManana() {
        try {
            despertar();
        } catch (RuntimeException e) {
            // Si falla hoy, que lo diga el log; mañana lo vuelve a intentar (y no repetirá lo ya enviado)
            log.error("El despertador falló", e);
        }
    }

    /** También lo usa el endpoint de administrador para la demo ("no vamos a esperar a las 6 a. m."). */
    public Resultado despertar() {
        // "hoy" en BOGOTÁ, no en la zona del servidor: el mismo motivo del zone del @Scheduled
        LocalDate hoy = LocalDate.now(zona);
        int vencidas = invitaciones.ejecutar(Instant.now());
        int enviados = recordatorios.ejecutar(hoy);
        log.info("Despertador {}: {} recordatorios nuevos, {} invitaciones vencidas", hoy, enviados, vencidas);
        return new Resultado(hoy, enviados, vencidas);
    }

    public record Resultado(LocalDate fecha, int recordatoriosEnviados, int invitacionesVencidas) {
    }
}
