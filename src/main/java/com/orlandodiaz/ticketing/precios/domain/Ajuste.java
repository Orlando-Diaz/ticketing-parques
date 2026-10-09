package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;

/** Lo que una regla le hace al precio: que tipo es, por que, y cuanto dinero representa. */
public record Ajuste(TipoAjuste tipo, String motivo, Dinero monto) {
}
