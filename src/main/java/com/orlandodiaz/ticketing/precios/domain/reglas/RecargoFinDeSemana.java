package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.Ajuste;
import com.orlandodiaz.ticketing.precios.domain.ContextoDePrecio;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TipoAjuste;
import java.time.DayOfWeek;
import java.util.Optional;

/** Sabados y domingos tienen un recargo del 10%. (Los festivos se agregan despues.) */
public class RecargoFinDeSemana implements ReglaDePrecio {

    private static final int PORCENTAJE = 10;

    @Override
    public Optional<Ajuste> evaluar(ContextoDePrecio c) {
        DayOfWeek dia = c.fechaVisita().getDayOfWeek();
        if (dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY) {
            return Optional.empty();
        }
        return Optional.of(new Ajuste(TipoAjuste.RECARGO, "Recargo fin de semana (10%)",
                c.precioBase().porcentaje(PORCENTAJE)));
    }
}
