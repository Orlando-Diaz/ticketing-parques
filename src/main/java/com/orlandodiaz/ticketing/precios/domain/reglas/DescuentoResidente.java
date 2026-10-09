package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.Ajuste;
import com.orlandodiaz.ticketing.precios.domain.ContextoDePrecio;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TipoAjuste;
import java.util.Optional;

/** Residentes del Quindio tienen 20% de descuento. */
public class DescuentoResidente implements ReglaDePrecio {

    private static final int PORCENTAJE = 20;

    @Override
    public Optional<Ajuste> evaluar(ContextoDePrecio c) {
        if (!c.residente()) {
            return Optional.empty();
        }
        return Optional.of(new Ajuste(TipoAjuste.DESCUENTO, "Descuento residente (20%)",
                c.precioBase().porcentaje(PORCENTAJE)));
    }
}
