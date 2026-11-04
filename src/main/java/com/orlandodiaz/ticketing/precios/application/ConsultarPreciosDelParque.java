package com.orlandodiaz.ticketing.precios.application;

import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;

/**
 * CASO DE USO: "ver como cobra un parque": sus categorias con precio y sus reglas activas.
 * Lo usa la pantalla de compra para mostrar un renglon por tipo de entrada.
 */
public class ConsultarPreciosDelParque {

    private final RepositorioDeConfiguracionDePrecios repositorio;

    public ConsultarPreciosDelParque(RepositorioDeConfiguracionDePrecios repositorio) {
        this.repositorio = repositorio;
    }

    public ConfiguracionDePrecios ejecutar(ParqueId parqueId) {
        return repositorio.buscarPorParque(parqueId)
                .orElseThrow(() -> new ParqueSinConfiguracionDePreciosException(parqueId));
    }
}
