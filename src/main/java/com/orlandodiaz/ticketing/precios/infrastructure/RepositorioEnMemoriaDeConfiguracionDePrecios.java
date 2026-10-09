package com.orlandodiaz.ticketing.precios.infrastructure;

import com.orlandodiaz.ticketing.precios.application.RepositorioDeConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ADAPTADOR (de salida) que implementa el puerto guardando en memoria.
 * Sirve para desarrollo y pruebas. Mas adelante habra otro adaptador con JPA/PostgreSQL
 * y el caso de uso no cambia ni una linea.
 */
public class RepositorioEnMemoriaDeConfiguracionDePrecios implements RepositorioDeConfiguracionDePrecios {

    private final Map<ParqueId, ConfiguracionDePrecios> almacen = new ConcurrentHashMap<>();

    @Override
    public Optional<ConfiguracionDePrecios> buscarPorParque(ParqueId parqueId) {
        return Optional.ofNullable(almacen.get(parqueId));
    }

    @Override
    public void guardar(ConfiguracionDePrecios configuracion) {
        almacen.put(configuracion.parqueId(), configuracion);
    }
}
