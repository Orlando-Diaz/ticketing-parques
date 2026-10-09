package com.orlandodiaz.ticketing.precios.application;

import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.util.Optional;

/**
 * PUERTO (de salida). La aplicacion dice QUE necesita ("buscame la configuracion de este parque")
 * pero no COMO se guarda: puede ser memoria, PostgreSQL, un archivo...
 * Quien lo implemente vive en infrastructure (principios D e I: interfaz chica, dependemos de ella).
 */
public interface RepositorioDeConfiguracionDePrecios {

    Optional<ConfiguracionDePrecios> buscarPorParque(ParqueId parqueId);

    void guardar(ConfiguracionDePrecios configuracion);
}
