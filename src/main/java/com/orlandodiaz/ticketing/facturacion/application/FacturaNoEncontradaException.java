package com.orlandodiaz.ticketing.facturacion.application;

public class FacturaNoEncontradaException extends RuntimeException {

    public FacturaNoEncontradaException(String ordenRef) {
        super("La orden '" + ordenRef + "' no tiene factura");
    }
}
