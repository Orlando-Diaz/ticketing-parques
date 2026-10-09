package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.Ajuste;
import com.orlandodiaz.ticketing.precios.domain.ContextoDePrecio;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TipoAjuste;
import java.util.Optional;

/** Descuento para residentes. El porcentaje lo decide cada parque. */
public class DescuentoResidente implements ReglaDePrecio {

    private final int porcentaje;

    public DescuentoResidente(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    @Override
    public Optional<Ajuste> evaluar(ContextoDePrecio c) {
        if (!c.residente()) {
            return Optional.empty();
        }
        return Optional.of(new Ajuste(TipoAjuste.DESCUENTO, "Descuento residente (" + porcentaje + "%)",
                c.precioBase().porcentaje(porcentaje)));
    }
}
