package com.orlandodiaz.ticketing.accesos.application;

import java.time.LocalDateTime;

/**
 * PUERTO: la hora actual, propio de este modulo (cada modulo es dueno de sus puertos y no se conocen entre si).
 * Decide que dia es "hoy" en la puerta del parque.
 */
@FunctionalInterface
public interface Reloj {
    LocalDateTime ahora();
}
