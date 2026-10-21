package com.orlandodiaz.ticketing.accesos.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** Una pasada por la puerta: cuantas personas entraron con el pase, cuando y por que puerta. */
public record Ingreso(int personas, String puerta, LocalDateTime momento) {

    public static final String PUERTA_POR_DEFECTO = "principal";

    public Ingreso {
        if (personas < 1) {
            throw new IllegalArgumentException("Entra al menos una persona");
        }
        Objects.requireNonNull(momento, "El momento del ingreso es obligatorio");
        puerta = (puerta == null || puerta.isBlank()) ? PUERTA_POR_DEFECTO : puerta.trim();
        if (puerta.length() > 100) {
            throw new IllegalArgumentException("El nombre de la puerta es demasiado largo (maximo 100)");
        }
    }
}
