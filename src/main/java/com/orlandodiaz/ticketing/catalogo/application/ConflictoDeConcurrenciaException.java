package com.orlandodiaz.ticketing.catalogo.application;

/**
 * El cupo cambio mientras lo estabamos modificando (otra compra se nos adelanto).
 * Es una excepcion del PUERTO: cada adaptador de persistencia traduce su error tecnico a esta,
 * y asi los casos de uso pueden reintentar sin saber si por debajo hay JPA, Mongo o memoria.
 */
public class ConflictoDeConcurrenciaException extends RuntimeException {

    public ConflictoDeConcurrenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public ConflictoDeConcurrenciaException(String mensaje) {
        super(mensaje);
    }
}
