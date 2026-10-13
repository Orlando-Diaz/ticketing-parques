package com.orlandodiaz.ticketing.ventas.infrastructure;

import com.orlandodiaz.ticketing.ventas.application.Reloj;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Adaptador de produccion: la hora real, en la zona horaria de Colombia (el servidor puede estar en UTC). */
public class RelojDelSistema implements Reloj {

    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    @Override
    public LocalDateTime ahora() {
        return LocalDateTime.now(COLOMBIA);
    }
}
