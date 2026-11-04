package com.orlandodiaz.ticketing.notificaciones.infrastructure;

import com.orlandodiaz.ticketing.notificaciones.application.EnviadorDeCorreo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** ADAPTADOR de desarrollo: sin SMTP configurado, el correo se escribe en el log para poder verlo. */
public class EnviadorPorConsola implements EnviadorDeCorreo {

    private static final Logger log = LoggerFactory.getLogger(EnviadorPorConsola.class);

    @Override
    public void enviar(String para, String asunto, String texto) {
        log.info("[correo simulado] Para: {} | Asunto: {}\n{}", para, asunto, texto);
    }
}
