package com.orlandodiaz.ticketing.precios.domain;

import java.time.LocalDate;

/** Lo que se sabe del visitante y de su visita al pedir un precio. */
public record DatosDeVisita(int edadVisitante, boolean residente, int tamanoGrupo, LocalDate fechaVisita) {

    public DatosDeVisita {
        if (edadVisitante < 0) {
            throw new IllegalArgumentException("La edad no puede ser negativa");
        }
        if (tamanoGrupo < 1) {
            throw new IllegalArgumentException("El grupo debe tener al menos 1 persona");
        }
        if (fechaVisita == null) {
            throw new IllegalArgumentException("La fecha de visita es obligatoria");
        }
    }
}
