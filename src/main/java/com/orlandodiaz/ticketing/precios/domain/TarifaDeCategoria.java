package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;

/** Cuanto cuesta la entrada para una categoria de visitante. */
public record TarifaDeCategoria(CategoriaVisitante categoria, Dinero precio) {
}
