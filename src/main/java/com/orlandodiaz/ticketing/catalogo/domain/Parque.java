package com.orlandodiaz.ticketing.catalogo.domain;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.Objects;

/** Un parque del sistema, con el cupo de visitantes que admite por dia por defecto. */
public record Parque(ParqueId id, String nombre, int cupoDiario) {

    public Parque {
        Objects.requireNonNull(id, "El id del parque es obligatorio");
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El parque necesita un nombre");
        }
        if (cupoDiario < 1) {
            throw new IllegalArgumentException("El cupo diario debe ser al menos 1");
        }
    }

    /** Crea la disponibilidad de una fecha usando el cupo por defecto del parque. */
    public DisponibilidadDiaria crearDisponibilidad(LocalDate fecha) {
        return DisponibilidadDiaria.nueva(id, fecha, cupoDiario);
    }
}
