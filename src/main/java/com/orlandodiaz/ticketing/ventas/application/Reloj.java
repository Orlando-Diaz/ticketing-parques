package com.orlandodiaz.ticketing.ventas.application;

import java.time.LocalDateTime;

/**
 * PUERTO: la hora actual. En vez de llamar LocalDateTime.now() dentro del codigo,
 * la pedimos por aqui: en produccion es el reloj real y en los tests es una fecha fija.
 */
@FunctionalInterface
public interface Reloj {
    LocalDateTime ahora();
}
