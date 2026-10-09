package com.orlandodiaz.ticketing.precios.application;

import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.domain.DatosDeVisita;
import com.orlandodiaz.ticketing.precios.domain.DesglosePrecio;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;

/**
 * CASO DE USO: "calcular el precio de una entrada en un parque".
 * Solo coordina: carga la configuracion del parque (via el puerto) y le pide el calculo al dominio.
 * No tiene reglas de negocio propias ni sabe nada de bases de datos o HTTP.
 */
public class CalcularPrecioDeEntrada {

    private final RepositorioDeConfiguracionDePrecios repositorio;

    public CalcularPrecioDeEntrada(RepositorioDeConfiguracionDePrecios repositorio) {
        this.repositorio = repositorio;
    }

    public DesglosePrecio ejecutar(ParqueId parqueId, DatosDeVisita visita) {
        ConfiguracionDePrecios configuracion = repositorio.buscarPorParque(parqueId)
                .orElseThrow(() -> new ParqueSinConfiguracionDePreciosException(parqueId));
        return configuracion.calcular(visita);
    }
}
