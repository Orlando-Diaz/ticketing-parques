package com.orlandodiaz.ticketing.ventas.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;

/** Una entrada dentro de una orden, con el precio que se cobro por ella. */
public record LineaDeOrden(int edadVisitante, boolean residente, Dinero precio) {
}
