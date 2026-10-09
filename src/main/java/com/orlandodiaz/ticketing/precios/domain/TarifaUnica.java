package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import java.util.Objects;

/** Un solo precio para todos los visitantes, sin importar la edad. */
public record TarifaUnica(Dinero precio) implements EsquemaDeTarifa {

    public TarifaUnica {
        Objects.requireNonNull(precio, "El precio es obligatorio");
    }

    @Override
    public Dinero precioPara(int edadVisitante) {
        return precio;
    }
}
