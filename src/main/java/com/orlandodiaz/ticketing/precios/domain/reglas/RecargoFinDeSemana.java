package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.Ajuste;
import com.orlandodiaz.ticketing.precios.domain.ContextoDePrecio;
import com.orlandodiaz.ticketing.precios.domain.DescripcionDeRegla;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TipoAjuste;
import java.time.DayOfWeek;
import java.util.Optional;

/** Recargo los sabados y domingos. El porcentaje lo decide cada parque. (Festivos: despues.) */
public class RecargoFinDeSemana implements ReglaDePrecio {

    public static final String TIPO = "RECARGO_FIN_DE_SEMANA";

    private final int porcentaje;

    public RecargoFinDeSemana(int porcentaje) {
        if (porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100: " + porcentaje);
        }
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

    @Override
    public DescripcionDeRegla describir() {
        return new DescripcionDeRegla(TIPO, porcentaje, null);
    }
}
