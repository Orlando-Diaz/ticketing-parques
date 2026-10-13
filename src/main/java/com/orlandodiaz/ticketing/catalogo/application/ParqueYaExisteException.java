package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;

public class ParqueYaExisteException extends RuntimeException {

    public ParqueYaExisteException(ParqueId id) {
        super("Ya existe un parque con el id '" + id.valor() + "'");
    }
}
