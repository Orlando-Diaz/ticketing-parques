package com.orlandodiaz.ticketing.precios.application;

import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;

/** CASO DE USO: definir (o reemplazar) como cobra un parque: su esquema de tarifa y sus reglas activas. */
public class ConfigurarPreciosDelParque {

    private final RepositorioDeConfiguracionDePrecios repositorio;

    public ConfigurarPreciosDelParque(RepositorioDeConfiguracionDePrecios repositorio) {
        this.repositorio = repositorio;
    }

    public void ejecutar(ConfiguracionDePrecios configuracion) {
        repositorio.guardar(configuracion);
    }
}
