package com.orlandodiaz.ticketing.ventas.domain;

import java.time.LocalDate;

public class FechaDeVisitaInvalidaException extends RuntimeException {

    public FechaDeVisitaInvalidaException(LocalDate fechaVisita, LocalDate hoy) {
        super("No se pueden comprar entradas para el " + fechaVisita + " (hoy es " + hoy + ")");
    }
}
