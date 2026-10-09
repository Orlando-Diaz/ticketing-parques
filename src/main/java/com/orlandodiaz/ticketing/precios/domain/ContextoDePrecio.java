package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import java.time.LocalDate;

/**
 * Los datos que las reglas necesitan para decidir. Las reglas SOLO leen esto,
 * asi no dependen de usuarios, bases de datos ni nada de infraestructura.
 */
public record ContextoDePrecio(
        Dinero precioBase,
        int edadVisitante,
        boolean residente,
        int tamanoGrupo,
        LocalDate fechaVisita) {

    public ContextoDePrecio {
        if (edadVisitante < 0) {
            throw new IllegalArgumentException("La edad no puede ser negativa");
        }
        if (tamanoGrupo < 1) {
            throw new IllegalArgumentException("El grupo debe tener al menos 1 persona");
        }
    }
}
