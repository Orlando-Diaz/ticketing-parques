package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.util.List;
import java.util.Objects;

/**
 * Como cobra UN parque: su esquema de tarifa y las reglas de ajuste que tiene activas.
 * Es lo que se guarda por parque y lo que se carga para calcular un precio.
 */
public record ConfiguracionDePrecios(ParqueId parqueId, EsquemaDeTarifa esquema, List<ReglaDePrecio> reglas) {

    public ConfiguracionDePrecios {
        Objects.requireNonNull(parqueId, "El parque es obligatorio");
        Objects.requireNonNull(esquema, "El esquema de tarifa es obligatorio");
        reglas = List.copyOf(reglas);
    }

    public DesglosePrecio calcular(DatosDeVisita visita) {
        return new CalculadorDePrecio(esquema, reglas).calcular(visita);
    }
}
