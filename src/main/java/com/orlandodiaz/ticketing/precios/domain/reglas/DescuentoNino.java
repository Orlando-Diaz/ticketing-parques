package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.Ajuste;
import com.orlandodiaz.ticketing.precios.domain.ContextoDePrecio;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TipoAjuste;
import java.util.Optional;

/** Ninos de 10 anos o menos pagan la mitad. */
public class DescuentoNino implements ReglaDePrecio {

    private static final int EDAD_MAXIMA = 10;
    private static final int PORCENTAJE = 50;

    @Override
    public Optional<Ajuste> evaluar(ContextoDePrecio c) {
        if (c.edadVisitante() > EDAD_MAXIMA) {
            return Optional.empty();
        }
        return Optional.of(new Ajuste(TipoAjuste.DESCUENTO, "Descuento nino (50%)",
                c.precioBase().porcentaje(PORCENTAJE)));
    }
}
