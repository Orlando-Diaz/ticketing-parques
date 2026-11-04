package com.orlandodiaz.ticketing.notificaciones.infrastructure;

import com.orlandodiaz.ticketing.notificaciones.application.EnviadorDeCorreo;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** ADAPTADOR: envia el correo por SMTP (Gmail u otro), con los datos de las variables MAIL_*. */
public class EnviadorSmtp implements EnviadorDeCorreo {

    private final JavaMailSender emisor;
    private final String remitente;

    public EnviadorSmtp(JavaMailSender emisor, String remitente) {
        this.emisor = emisor;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String para, String asunto, String texto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(para);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        emisor.send(mensaje);
    }
}
