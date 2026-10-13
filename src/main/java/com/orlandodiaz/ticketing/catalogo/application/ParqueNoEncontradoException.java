package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;

public class ParqueNoEncontradoException extends RuntimeException {

    public ParqueNoEncontradoException(ParqueId id) {
        super("No existe el parque '" + id.valor() + "'");
    }
}
