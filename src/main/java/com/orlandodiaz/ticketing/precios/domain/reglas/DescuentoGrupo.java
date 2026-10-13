package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.Ajuste;
import com.orlandodiaz.ticketing.precios.domain.ContextoDePrecio;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TipoAjuste;
import java.util.Optional;

/** Descuento por persona para grupos. Tamano minimo y porcentaje los decide cada parque. */
public class DescuentoGrupo implements ReglaDePrecio {

    private final int tamanoMinimo;
    private final int porcentaje;

    public DescuentoGrupo(int tamanoMinimo, int porcentaje) {
        if (tamanoMinimo < 1) {
            throw new IllegalArgumentException("El tamano minimo del grupo debe ser al menos 1");
        }
        if (porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100: " + porcentaje);
        }
        this.tamanoMinimo = tamanoMinimo;
        this.porcentaje = porcentaje;
    }

    @Override
    public Optional<Ajuste> evaluar(ContextoDePrecio c) {
        if (c.tamanoGrupo() < tamanoMinimo) {
            return Optional.empty();
        }
        return Optional.of(new Ajuste(TipoAjuste.DESCUENTO,
                "Descuento grupo de " + tamanoMinimo + " o mas (" + porcentaje + "%)",
                c.precioBase().porcentaje(porcentaje)));
    }
}
