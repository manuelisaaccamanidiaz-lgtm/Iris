package com.iris.infrastructure.adapter.out.notificacion;

import org.springframework.stereotype.Component;

import com.iris.application.port.out.Notificador;
import com.iris.domain.notificacion.Notificacion;
import com.iris.domain.notificacion.NotificacionRepositorio;
import com.iris.infrastructure.config.CorreoPropiedades;

/**
 * El adaptador del puerto Notificador. "Compuesto" porque reparte cada aviso a DOS canales:
 * <ol>
 *   <li>la campanita (tabla notificaciones), si la persona tiene cuenta;</li>
 *   <li>el correo, si el tipo lo merece (TipoNotificacion.enviaCorreo) y el interruptor está encendido.</li>
 * </ol>
 * Para agregar un canal (WhatsApp, Telegram...) se toca SOLO esta clase: los casos de uso ni se enteran.
 */
@Component
class NotificadorCompuesto implements Notificador {

    private final NotificacionRepositorio notificaciones;
    private final EnviadorCorreo correo;
    private final CorreoPropiedades propiedades;

    NotificadorCompuesto(NotificacionRepositorio notificaciones, EnviadorCorreo correo,
                         CorreoPropiedades propiedades) {
        this.notificaciones = notificaciones;
        this.correo = correo;
        this.propiedades = propiedades;
    }

    @Override
    public void notificar(Aviso a) {
        if (a.usuarioId() != null) {
            notificaciones.guardar(Notificacion.crear(a.usuarioId(), a.eventoId(), a.tipo(), a.mensaje(),
                    a.referenciaId()));
        }
        if (propiedades.habilitado() && a.tipo().enviaCorreo() && a.email() != null) {
            PlantillaCorreo.Correo c = PlantillaCorreo.para(a, propiedades.urlApp());
            correo.enviar(a.email(), c.asunto(), c.textoPlano(), c.html());
        }
    }
}
