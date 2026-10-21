package com.orlandodiaz.ticketing.accesos.domain;

public class PaseAnuladoException extends RuntimeException {

    public PaseAnuladoException() {
        super("Este pase fue anulado porque la compra se cancelo");
    }
}
