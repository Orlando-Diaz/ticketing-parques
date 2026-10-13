package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAIZ DE COMPOSICION: las clases del dominio y de application no tienen anotaciones de Spring.
 * Aqui (y en las otras clases *Config) es donde le decimos a Spring como construirlas y conectarlas.
 * El bus es tambien un PublicadorDeEventos, asi que un solo bean sirve para ambos tipos.
 */
@Configuration
public class EventosConfig {

    @Bean
    BusDeEventosEnMemoria busDeEventos() {
        return new BusDeEventosEnMemoria();
    }
}
