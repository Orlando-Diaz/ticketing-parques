package com.orlandodiaz.ticketing.ventas.domain;

/** Una persona que va a entrar al parque con una de las entradas de la compra. */
public record Visitante(int edad, boolean residente) {

    public Visitante {
        if (edad < 0) {
            throw new IllegalArgumentException("La edad no puede ser negativa");
        }
    }
}
