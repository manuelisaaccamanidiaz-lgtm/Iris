package com.iris.domain.notificacion;

import java.time.LocalDate;
import java.util.UUID;

/** La "memoria" del despertador: qué recordatorios ya se enviaron (tabla recordatorios_enviados). */
public interface RecordatorioRepositorio {

    /**
     * Intenta registrar el recordatorio. true = es NUEVO (hay que enviarlo);
     * false = ya se había enviado (no se repite). Lo decide la PK de la V11, no un SELECT previo,
     * así dos procesos simultáneos nunca envían el mismo aviso.
     */
    boolean registrarSiEsNuevo(UUID tareaId, TipoNotificacion tipo, LocalDate fechaLimite);
}
