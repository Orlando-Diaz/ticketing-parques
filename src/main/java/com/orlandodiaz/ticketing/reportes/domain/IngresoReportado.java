package com.orlandodiaz.ticketing.reportes.domain;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Hecho para reportes: una pasada por la puerta. (ordenRef, numero) la identifica de forma unica. */
public record IngresoReportado(String ordenRef, int numero, ParqueId parqueId, LocalDate fechaVisita,
                               int personas, String puerta, LocalDateTime momento) {

    public IngresoReportado {
        if (ordenRef == null || ordenRef.isBlank()) {
            throw new IllegalArgumentException("La orden es obligatoria");
        }
        Objects.requireNonNull(parqueId, "El parque es obligatorio");
        Objects.requireNonNull(fechaVisita, "La fecha de visita es obligatoria");
        Objects.requireNonNull(momento, "El momento es obligatorio");
        if (numero < 0) {
            throw new IllegalArgumentException("El numero de ingreso no puede ser negativo");
        }
        if (personas < 1) {
            throw new IllegalArgumentException("Entra al menos una persona");
        }
        if (puerta == null || puerta.isBlank()) {
            throw new IllegalArgumentException("La puerta es obligatoria");
        }
    }
}
