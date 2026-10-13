package com.orlandodiaz.ticketing.ventas.infrastructure;

import com.orlandodiaz.ticketing.precios.application.CalcularPrecioDeEntrada;
import com.orlandodiaz.ticketing.precios.domain.DatosDeVisita;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.ventas.application.PreciosDeEntrada;
import com.orlandodiaz.ticketing.ventas.domain.Visitante;
import java.time.LocalDate;

/**
 * ADAPTADOR que conecta ventas con el modulo precios. Es el UNICO lugar de ventas que conoce a precios:
 * traduce el lenguaje de ventas (Visitante) al de precios (DatosDeVisita).
 */
public class PreciosDeEntradaDesdeModuloPrecios implements PreciosDeEntrada {

    private final CalcularPrecioDeEntrada calcularPrecio;

    public PreciosDeEntradaDesdeModuloPrecios(CalcularPrecioDeEntrada calcularPrecio) {
        this.calcularPrecio = calcularPrecio;
    }

    @Override
    public Dinero precioPara(ParqueId parqueId, Visitante visitante, int tamanoGrupo, LocalDate fechaVisita) {
        var datos = new DatosDeVisita(visitante.edad(), visitante.residente(), tamanoGrupo, fechaVisita);
        return calcularPrecio.ejecutar(parqueId, datos).total();
    }
}
