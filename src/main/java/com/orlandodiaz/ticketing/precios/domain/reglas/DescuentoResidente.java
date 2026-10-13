package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.Ajuste;
import com.orlandodiaz.ticketing.precios.domain.ContextoDePrecio;
import com.orlandodiaz.ticketing.precios.domain.DescripcionDeRegla;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TipoAjuste;
import java.util.Optional;

/** Descuento para residentes. El porcentaje lo decide cada parque. */
public class DescuentoResidente implements ReglaDePrecio {

    public static final String TIPO = "DESCUENTO_RESIDENTE";

    private final int porcentaje;

    public DescuentoResidente(int porcentaje) {
        if (porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100: " + porcentaje);
        }
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

    @Override
    public DescripcionDeRegla describir() {
        return new DescripcionDeRegla(TIPO, porcentaje, null);
    }
}
