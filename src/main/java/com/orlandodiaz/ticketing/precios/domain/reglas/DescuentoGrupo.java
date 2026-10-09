package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.Ajuste;
import com.orlandodiaz.ticketing.precios.domain.ContextoDePrecio;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TipoAjuste;
import java.util.Optional;

/** Grupos de 10 o mas personas tienen 15% de descuento por persona. */
public class DescuentoGrupo implements ReglaDePrecio {

    private static final int TAMANO_MINIMO = 10;
    private static final int PORCENTAJE = 15;

    @Override
    public Optional<Ajuste> evaluar(ContextoDePrecio c) {
        if (c.tamanoGrupo() < TAMANO_MINIMO) {
            return Optional.empty();
        }
        return Optional.of(new Ajuste(TipoAjuste.DESCUENTO, "Descuento grupo (15%)",
                c.precioBase().porcentaje(PORCENTAJE)));
    }
}
