package com.orlandodiaz.ticketing.shared.application;

/**
 * Alguien modifico el mismo dato mientras lo estabamos modificando (otra compra o cancelacion se nos adelanto).
 * Es una excepcion del PUERTO: cada adaptador de persistencia traduce su error tecnico a esta,
 * y asi los casos de uso pueden reintentar sin saber si por debajo hay JPA, Mongo o memoria.
 * Vive en shared porque la usan varios modulos (catalogo y ventas) que no deben conocerse entre si.
 */
public class ConflictoDeConcurrenciaException extends RuntimeException {

    public ConflictoDeConcurrenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public ConflictoDeConcurrenciaException(String mensaje) {
        super(mensaje);
    }
}
