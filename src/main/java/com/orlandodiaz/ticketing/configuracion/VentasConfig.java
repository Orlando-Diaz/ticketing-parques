package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.catalogo.application.LiberarCupo;
import com.orlandodiaz.ticketing.catalogo.application.ReservarCupo;
import com.orlandodiaz.ticketing.precios.application.CalcularPrecioDeEntrada;
import com.orlandodiaz.ticketing.shared.application.PublicadorDeEventos;
import com.orlandodiaz.ticketing.ventas.application.CancelarOrden;
import com.orlandodiaz.ticketing.ventas.application.ComprarEntradas;
import com.orlandodiaz.ticketing.ventas.application.ConsultarOrden;
import com.orlandodiaz.ticketing.ventas.application.CupoDeParque;
import com.orlandodiaz.ticketing.ventas.application.PreciosDeEntrada;
import com.orlandodiaz.ticketing.ventas.application.Reloj;
import com.orlandodiaz.ticketing.ventas.application.RepositorioDeOrdenes;
import com.orlandodiaz.ticketing.ventas.infrastructure.CupoDeParqueDesdeModuloCatalogo;
import com.orlandodiaz.ticketing.ventas.infrastructure.PreciosDeEntradaDesdeModuloPrecios;
import com.orlandodiaz.ticketing.ventas.infrastructure.RelojDelSistema;
import com.orlandodiaz.ticketing.ventas.infrastructure.RepositorioEnMemoriaDeOrdenes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VentasConfig {

    @Bean
    Reloj reloj() {
        return new RelojDelSistema();
    }

    @Bean
    RepositorioDeOrdenes repositorioDeOrdenes() {
        return new RepositorioEnMemoriaDeOrdenes();
    }

    // Los dos adaptadores que conectan ventas con los otros modulos
    @Bean
    PreciosDeEntrada preciosDeEntrada(CalcularPrecioDeEntrada calcularPrecio) {
        return new PreciosDeEntradaDesdeModuloPrecios(calcularPrecio);
    }

    @Bean
    CupoDeParque cupoDeParque(ReservarCupo reservarCupo, LiberarCupo liberarCupo) {
        return new CupoDeParqueDesdeModuloCatalogo(reservarCupo, liberarCupo);
    }

    @Bean
    ComprarEntradas comprarEntradas(PreciosDeEntrada precios, CupoDeParque cupo, RepositorioDeOrdenes ordenes,
                                    Reloj reloj, PublicadorDeEventos eventos) {
        return new ComprarEntradas(precios, cupo, ordenes, reloj, eventos);
    }

    @Bean
    CancelarOrden cancelarOrden(RepositorioDeOrdenes ordenes, CupoDeParque cupo, Reloj reloj,
                                PublicadorDeEventos eventos) {
        return new CancelarOrden(ordenes, cupo, reloj, eventos);
    }

    @Bean
    ConsultarOrden consultarOrden(RepositorioDeOrdenes ordenes) {
        return new ConsultarOrden(ordenes);
    }
}
