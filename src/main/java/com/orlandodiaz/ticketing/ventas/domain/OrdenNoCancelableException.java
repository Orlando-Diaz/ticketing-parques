package com.orlandodiaz.ticketing.ventas.domain;

public class OrdenNoCancelableException extends RuntimeException {

    public OrdenNoCancelableException(String motivo) {
        super(motivo);
    }
}
