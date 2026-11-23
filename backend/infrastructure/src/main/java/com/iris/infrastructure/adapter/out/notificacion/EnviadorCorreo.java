package com.iris.infrastructure.adapter.out.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.iris.infrastructure.config.CorreoPropiedades;

/**
 * Envía UN correo de texto por SMTP (Mailpit en desarrollo, Gmail después: solo cambia el .env).
 * @Async: corre en el hilo del "cartero", así quien hizo la petición no espera al servidor de correo.
 * Está en su propio bean porque @Async no funciona si un método se llama a sí mismo dentro de la misma clase.
 */
@Component
class EnviadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(EnviadorCorreo.class);

    private final JavaMailSender mail;
    private final CorreoPropiedades propiedades;

    EnviadorCorreo(JavaMailSender mail, CorreoPropiedades propiedades) {
        this.mail = mail;
        this.propiedades = propiedades;
    }

    @Async("correoExecutor")
    void enviar(String para, String asunto, String cuerpo) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(propiedades.remitente());
        m.setTo(para);
        m.setSubject(asunto);
        m.setText(cuerpo);
        try {
            mail.send(m);
            log.info("Correo enviado a {}: {}", para, asunto);
        } catch (MailException e) {
            // Tolerancia a fallos: el correo es un extra; la campanita ya tiene el aviso
            log.warn("No se pudo enviar el correo a {}: {}", para, e.getMessage());
        }
    }
}
