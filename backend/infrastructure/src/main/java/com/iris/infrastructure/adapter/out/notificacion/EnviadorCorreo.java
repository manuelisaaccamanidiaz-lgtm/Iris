package com.iris.infrastructure.adapter.out.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.iris.infrastructure.config.CorreoPropiedades;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envía UN correo por SMTP (Mailpit en desarrollo, Gmail después: solo cambia el .env).
 * Va en dos versiones dentro del mismo mensaje ("multipart/alternative"): HTML con diseño y
 * texto plano, para los programas de correo que no muestran HTML.
 * @Async: corre en el hilo del "cartero", así quien hizo la petición no espera al servidor de correo.
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
    void enviar(String para, String asunto, String textoPlano, String html) {
        try {
            MimeMessage mensaje = mail.createMimeMessage();
            // true = multipart (texto + HTML); UTF-8 para tildes y eñes
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, "UTF-8");
            ayudante.setFrom(propiedades.remitente());
            ayudante.setTo(para);
            ayudante.setSubject(asunto);
            ayudante.setText(textoPlano, html);
            mail.send(mensaje);
            log.info("Correo enviado a {}: {}", para, asunto);
        } catch (MailException | MessagingException e) {
            // Tolerancia a fallos: el correo es un extra; la campanita ya tiene el aviso
            log.warn("No se pudo enviar el correo a {}: {}", para, e.getMessage());
        }
    }
}
