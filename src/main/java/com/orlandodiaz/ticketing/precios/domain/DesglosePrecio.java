package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import java.util.List;

/** Resultado del calculo: precio base, los ajustes aplicados y el total final. */
public record DesglosePrecio(Dinero precioBase, List<Ajuste> ajustesAplicados, Dinero total) {
}
