package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.Ajuste;
import com.orlandodiaz.ticketing.precios.domain.ContextoDePrecio;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TipoAjuste;
import java.util.Optional;

/** Mayores de 60 anos (inclusive) tienen 30% de descuento. */
public class DescuentoAdultoMayor implements ReglaDePrecio {

    private static final int EDAD_MINIMA = 60;
    private static final int PORCENTAJE = 30;

    @Override
    public Optional<Ajuste> evaluar(ContextoDePrecio c) {
        if (c.edadVisitante() < EDAD_MINIMA) {
            return Optional.empty();
        }
        return Optional.of(new Ajuste(TipoAjuste.DESCUENTO, "Descuento adulto mayor (30%)",
                c.precioBase().porcentaje(PORCENTAJE)));
    }
}
