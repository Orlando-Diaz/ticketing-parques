package com.orlandodiaz.ticketing.accesos.application;

public class PaseNoEncontradoException extends RuntimeException {

    private PaseNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    /** No se dice cual codigo se probo: asi no se ayuda a quien intenta adivinar codigos. */
    public static PaseNoEncontradoException paraCodigo() {
        return new PaseNoEncontradoException("Codigo de pase no valido");
    }

    public static PaseNoEncontradoException paraOrden(String ordenRef) {
        return new PaseNoEncontradoException("La orden '" + ordenRef + "' no tiene pase");
    }
}
