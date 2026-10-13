package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.facturacion.application.AnularFacturaDeOrden;
import com.orlandodiaz.ticketing.facturacion.application.ConsultarFacturaDeOrden;
import com.orlandodiaz.ticketing.facturacion.application.EmitirFactura;
import com.orlandodiaz.ticketing.facturacion.application.NumeradorDeFacturas;
import com.orlandodiaz.ticketing.facturacion.application.RepositorioDeFacturas;
import com.orlandodiaz.ticketing.facturacion.infrastructure.EscuchaDeEventosDeVentas;
import com.orlandodiaz.ticketing.facturacion.infrastructure.persistence.ConsecutivoJpaRepository;
import com.orlandodiaz.ticketing.facturacion.infrastructure.persistence.FacturaJpaRepository;
import com.orlandodiaz.ticketing.facturacion.infrastructure.persistence.NumeradorConConsecutivoEnBaseDeDatos;
import com.orlandodiaz.ticketing.facturacion.infrastructure.persistence.RepositorioJpaDeFacturas;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FacturacionConfig {

    @Bean
    RepositorioDeFacturas repositorioDeFacturas(FacturaJpaRepository jpa) {
        return new RepositorioJpaDeFacturas(jpa);
    }

    @Bean
    NumeradorDeFacturas numeradorDeFacturas(ConsecutivoJpaRepository jpa) {
        return new NumeradorConConsecutivoEnBaseDeDatos(jpa);
    }

    @Bean
    EmitirFactura emitirFactura(RepositorioDeFacturas facturas, NumeradorDeFacturas numerador) {
        return new EmitirFactura(facturas, numerador);
    }

    @Bean
    AnularFacturaDeOrden anularFacturaDeOrden(RepositorioDeFacturas facturas) {
        return new AnularFacturaDeOrden(facturas);
    }

    @Bean
    ConsultarFacturaDeOrden consultarFacturaDeOrden(RepositorioDeFacturas facturas) {
        return new ConsultarFacturaDeOrden(facturas);
    }

    /** Al crearse, esta escucha se suscribe a los eventos de ventas en el bus. */
    @Bean
    EscuchaDeEventosDeVentas escuchaDeEventosDeVentas(EmitirFactura emitirFactura,
                                                      AnularFacturaDeOrden anularFactura,
                                                      BusDeEventosEnMemoria bus) {
        var escucha = new EscuchaDeEventosDeVentas(emitirFactura, anularFactura);
        escucha.registrarEn(bus);
        return escucha;
    }
}
