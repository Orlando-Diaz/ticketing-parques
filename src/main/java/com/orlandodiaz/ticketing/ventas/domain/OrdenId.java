package com.orlandodiaz.ticketing.ventas.domain;

import java.util.UUID;

public record OrdenId(String valor) {

    public OrdenId {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id de la orden es obligatorio");
        }
    }

    public static OrdenId nuevo() {
        return new OrdenId(UUID.randomUUID().toString());
    }
}
