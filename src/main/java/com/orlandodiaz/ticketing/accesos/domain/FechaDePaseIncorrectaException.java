package com.orlandodiaz.ticketing.accesos.domain;

import java.time.LocalDate;

public class FechaDePaseIncorrectaException extends RuntimeException {

    public FechaDePaseIncorrectaException(LocalDate fechaVisita, LocalDate hoy) {
        super(hoy.isBefore(fechaVisita)
                ? "Este pase es valido el " + fechaVisita + " (hoy es " + hoy + ")"
                : "Este pase vencio: era valido el " + fechaVisita);
    }
}
