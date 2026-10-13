package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.ventas.domain.OrdenId;

public class OrdenNoEncontradaException extends RuntimeException {

    public OrdenNoEncontradaException(OrdenId id) {
        super("No existe la orden '" + id.valor() + "'");
    }
}
