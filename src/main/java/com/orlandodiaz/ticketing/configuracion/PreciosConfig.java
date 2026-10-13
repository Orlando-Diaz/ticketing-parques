package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.precios.application.CalcularPrecioDeEntrada;
import com.orlandodiaz.ticketing.precios.application.ConfigurarPreciosDelParque;
import com.orlandodiaz.ticketing.precios.application.RepositorioDeConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.infrastructure.RepositorioEnMemoriaDeConfiguracionDePrecios;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PreciosConfig {

    @Bean
    RepositorioDeConfiguracionDePrecios repositorioDeConfiguracionDePrecios() {
        return new RepositorioEnMemoriaDeConfiguracionDePrecios();
    }

    @Bean
    CalcularPrecioDeEntrada calcularPrecioDeEntrada(RepositorioDeConfiguracionDePrecios repositorio) {
        return new CalcularPrecioDeEntrada(repositorio);
    }

    @Bean
    ConfigurarPreciosDelParque configurarPreciosDelParque(RepositorioDeConfiguracionDePrecios repositorio) {
        return new ConfigurarPreciosDelParque(repositorio);
    }
}
