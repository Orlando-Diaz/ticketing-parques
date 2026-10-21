package com.orlandodiaz.ticketing.accesos.application;

/**
 * PUERTO: de donde salen los codigos de los pases. Deben ser imposibles de adivinar: quien conozca el codigo
 * de otra compra podria usar sus entradas. En produccion es un generador aleatorio seguro; en los tests, uno fijo.
 */
@FunctionalInterface
public interface GeneradorDeCodigos {
    String siguiente();
}
