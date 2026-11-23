package com.iris.infrastructure.adapter.out.notificacion;

import org.springframework.web.util.HtmlUtils;

import com.iris.application.port.out.Notificador.Aviso;

/**
 * Arma el correo (asunto, HTML y texto plano) de cada aviso.
 *
 * <p>SEGURIDAD: el mensaje incluye textos que escribió un usuario (el título de una tarea, el
 * nombre de un evento). Antes de meterlos en el HTML se ESCAPAN (HtmlUtils.htmlEscape): si alguien
 * llama a su tarea "&lt;script&gt;...", en el correo se ve como texto y no se ejecuta.
 *
 * <p>El botón NUNCA hace la acción (aceptar, completar): solo abre Iris. Los antivirus de correo
 * abren los enlaces automáticamente para revisarlos; si el enlace aceptara, aceptarían por ti.
 */
final class PlantillaCorreo {

    private PlantillaCorreo() {
    }

    record Correo(String asunto, String html, String textoPlano) {
    }

    static Correo para(Aviso a, String urlApp) {
        String titulo;
        String boton;
        String enlace;
        switch (a.tipo()) {
            case INVITACION_RECIBIDA -> {
                titulo = "Tienes una invitación";
                boton = "Ver invitación";
                enlace = urlApp + "/invitaciones";
            }
            case TAREA_VENCE_MANANA -> {
                titulo = "Una tarea vence mañana";
                boton = "Ver mis tareas";
                enlace = urlApp + "/eventos/" + a.eventoId() + "/tareas";
            }
            case TAREA_VENCE_HOY -> {
                titulo = "Una tarea vence hoy";
                boton = "Ver mis tareas";
                enlace = urlApp + "/eventos/" + a.eventoId() + "/tareas";
            }
            case TAREA_VENCIDA -> {
                titulo = "Tienes una tarea vencida";
                boton = "Ver mis tareas";
                enlace = urlApp + "/eventos/" + a.eventoId() + "/tareas";
            }
            default -> {
                titulo = "Novedades en tu evento";
                boton = "Abrir Iris";
                enlace = urlApp;
            }
        }
        String mensaje = HtmlUtils.htmlEscape(a.mensaje());
        // Estilos "en línea" (style="..."): muchos programas de correo ignoran las etiquetas <style>
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;background:#f4f4f7;padding:24px">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden">
                    <div style="background:#5b3cc4;color:#ffffff;padding:18px 24px;font-size:20px;font-weight:bold">
                      Iris
                    </div>
                    <div style="padding:24px;color:#222222">
                      <h2 style="margin:0 0 12px 0;font-size:18px">%s</h2>
                      <p style="margin:0 0 20px 0;line-height:1.5">%s</p>
                      <a href="%s" style="display:inline-block;background:#5b3cc4;color:#ffffff;
                         text-decoration:none;padding:12px 20px;border-radius:8px;font-weight:bold">%s</a>
                      <p style="margin:20px 0 0 0;font-size:12px;color:#777777">
                        Si el botón no funciona, copia este enlace en tu navegador: %s
                      </p>
                    </div>
                    <div style="padding:14px 24px;background:#fafafa;font-size:12px;color:#777777">
                      Soy Iris, una inteligencia artificial que ayuda a organizar eventos.
                      Recibes este correo porque perteneces a un evento en Iris (o te invitaron a uno).
                    </div>
                  </div>
                </div>
                """.formatted(HtmlUtils.htmlEscape(titulo), mensaje, enlace, boton, enlace);
        String texto = titulo + "\n\n" + a.mensaje() + "\n\n" + boton + ": " + enlace
                + "\n\n— Iris, tu asistente de eventos (soy una IA)";
        return new Correo("Iris: " + titulo.toLowerCase(), html, texto);
    }
}
