package com.orlandodiaz.ticketing.shared.application;

import java.util.function.Supplier;

/**
 * PUERTO: "ejecuta este trabajo como UNA sola unidad: o se guarda todo, o no se guarda nada".
 *
 * Los casos de uso no conocen Spring, asi que piden la transaccion por este puerto. Lo usan para que
 * "guardar el agregado" y "anotar el evento en el outbox" ocurran juntos (ver ADR 0007).
 * Quien lo llame debe reintentar FUERA de la transaccion, nunca dentro.
 */
@FunctionalInterface
public interface EjecutorTransaccional {

    /** Para pruebas en memoria y para quien no necesita transaccion: ejecuta el trabajo tal cual. */
    EjecutorTransaccional SIN_TRANSACCION = new EjecutorTransaccional() {
        @Override
        public <T> T ejecutar(Supplier<T> trabajo) {
            return trabajo.get();
        }
    };

    <T> T ejecutar(Supplier<T> trabajo);
}
