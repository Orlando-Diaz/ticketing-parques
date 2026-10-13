package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.ventas.domain.Visitante;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Los datos que entran al caso de uso de comprar (lo que luego mandara la API). */
public record ComprarEntradasComando(ParqueId parqueId, LocalDate fechaVisita, List<Visitante> visitantes) {

    public ComprarEntradasComando {
        Objects.requireNonNull(parqueId, "El parque es obligatorio");
        Objects.requireNonNull(fechaVisita, "La fecha de visita es obligatoria");
        if (visitantes == null || visitantes.isEmpty()) {
            throw new IllegalArgumentException("Debe haber al menos un visitante");
        }
        visitantes = List.copyOf(visitantes);
    }
}
