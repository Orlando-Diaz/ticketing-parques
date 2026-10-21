package com.orlandodiaz.ticketing.reportes.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Hecho para reportes: una orden de compra (el reporte no necesita saber mas de ella). */
public record OrdenReportada(String ordenRef, ParqueId parqueId, LocalDate fechaVisita, int entradas,
                             Dinero total, boolean cancelada, LocalDateTime registradaEn) {

    public OrdenReportada {
        if (ordenRef == null || ordenRef.isBlank()) {
            throw new IllegalArgumentException("La orden es obligatoria");
        }
        Objects.requireNonNull(parqueId, "El parque es obligatorio");
        Objects.requireNonNull(fechaVisita, "La fecha de visita es obligatoria");
        Objects.requireNonNull(total, "El total es obligatorio");
        Objects.requireNonNull(registradaEn, "La fecha de registro es obligatoria");
        if (entradas < 1) {
            throw new IllegalArgumentException("La orden necesita al menos una entrada");
        }
    }
}
