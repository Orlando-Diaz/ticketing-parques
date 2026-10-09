package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.Ajuste;
import com.orlandodiaz.ticketing.precios.domain.ContextoDePrecio;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TipoAjuste;
import java.time.DayOfWeek;
import java.util.Optional;

/** Recargo los sabados y domingos. El porcentaje lo decide cada parque. (Festivos: despues.) */
public class RecargoFinDeSemana implements ReglaDePrecio {

    private final int porcentaje;

    public RecargoFinDeSemana(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    @Override
    public Optional<Ajuste> evaluar(ContextoDePrecio c) {
        DayOfWeek dia = c.fechaVisita().getDayOfWeek();
        if (dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY) {
            return Optional.empty();
        }
        return Optional.of(new Ajuste(TipoAjuste.RECARGO, "Recargo fin de semana (" + porcentaje + "%)",
                c.precioBase().porcentaje(porcentaje)));
    }
}
