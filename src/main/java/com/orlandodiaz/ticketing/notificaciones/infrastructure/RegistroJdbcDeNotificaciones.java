package com.orlandodiaz.ticketing.notificaciones.infrastructure;

import com.orlandodiaz.ticketing.notificaciones.application.RegistroDeNotificaciones;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** ADAPTADOR: guarda en la tabla "notificacion" que avisos ya salieron. */
public class RegistroJdbcDeNotificaciones implements RegistroDeNotificaciones {

    private final JdbcTemplate jdbc;

    public RegistroJdbcDeNotificaciones(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean yaEnviada(String clave) {
        Integer n = jdbc.queryForObject("select count(*) from notificacion where clave = ?", Integer.class, clave);
        return n != null && n > 0;
    }

    @Override
    public void marcarEnviada(String clave) {
        try {
            jdbc.update("insert into notificacion (clave, creada_en) values (?, ?)", clave, LocalDateTime.now());
        } catch (DuplicateKeyException e) {
            // otro hilo ya lo marco: da igual
        }
    }
}
