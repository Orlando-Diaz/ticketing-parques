package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.catalogo.application.ListarParques;
import com.orlandodiaz.ticketing.notificaciones.application.EnviadorDeCorreo;
import com.orlandodiaz.ticketing.notificaciones.application.NombresDeParques;
import com.orlandodiaz.ticketing.notificaciones.application.NotificarCompraConfirmada;
import com.orlandodiaz.ticketing.notificaciones.application.RegistroDeNotificaciones;
import com.orlandodiaz.ticketing.notificaciones.infrastructure.EnviadorPorConsola;
import com.orlandodiaz.ticketing.notificaciones.infrastructure.EnviadorSmtp;
import com.orlandodiaz.ticketing.notificaciones.infrastructure.EscuchaDeCompras;
import com.orlandodiaz.ticketing.notificaciones.infrastructure.NombresDeParquesDelCatalogo;
import com.orlandodiaz.ticketing.notificaciones.infrastructure.RegistroJdbcDeNotificaciones;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Compone el modulo de notificaciones. Si defines MAIL_USER y MAIL_PASSWORD el correo sale por SMTP (Gmail por
 * defecto); si no, se escribe en el log y la app funciona igual.
 */
@Configuration
public class NotificacionesConfig {

    @Bean
    EnviadorDeCorreo enviadorDeCorreo(@Value("${ticketing.correo.host}") String host,
                                      @Value("${ticketing.correo.puerto}") int puerto,
                                      @Value("${ticketing.correo.usuario:}") String usuario,
                                      @Value("${ticketing.correo.clave:}") String clave,
                                      @Value("${ticketing.correo.remitente:}") String remitente) {
        if (usuario.isBlank() || clave.isBlank()) {
            return new EnviadorPorConsola();
        }
        JavaMailSenderImpl emisor = new JavaMailSenderImpl();
        emisor.setHost(host);
        emisor.setPort(puerto);
        emisor.setUsername(usuario);
        emisor.setPassword(clave);
        emisor.getJavaMailProperties().put("mail.smtp.auth", "true");
        emisor.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
        emisor.getJavaMailProperties().put("mail.smtp.connectiontimeout", "5000");
        emisor.getJavaMailProperties().put("mail.smtp.timeout", "10000");
        emisor.getJavaMailProperties().put("mail.smtp.writetimeout", "10000");
        return new EnviadorSmtp(emisor, remitente.isBlank() ? usuario : remitente);
    }

    @Bean
    NombresDeParques nombresDeParques(ListarParques listarParques) {
        return new NombresDeParquesDelCatalogo(listarParques);
    }

    @Bean
    RegistroDeNotificaciones registroDeNotificaciones(JdbcTemplate jdbc) {
        return new RegistroJdbcDeNotificaciones(jdbc);
    }

    @Bean
    NotificarCompraConfirmada notificarCompraConfirmada(EnviadorDeCorreo correo, NombresDeParques parques,
                                                        RegistroDeNotificaciones registro,
                                                        @Value("${ticketing.frontend.url}") String urlDelFrontend) {
        return new NotificarCompraConfirmada(correo, parques, registro, urlDelFrontend);
    }

    @Bean
    EscuchaDeCompras escuchaDeCompras(NotificarCompraConfirmada notificar, BusDeEventosEnMemoria bus) {
        var escucha = new EscuchaDeCompras(notificar);
        escucha.registrarEn(bus);
        return escucha;
    }
}
