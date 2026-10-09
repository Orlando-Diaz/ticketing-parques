package com.orlandodiaz.ticketing.shared.domain;

/** Identificador de un parque. Esta en shared porque varios modulos lo usan. */
public record ParqueId(String valor) {

    public ParqueId {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id del parque es obligatorio");
        }
    }
}
