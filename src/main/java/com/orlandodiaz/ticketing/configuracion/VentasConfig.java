package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.catalogo.application.LiberarCupo;
import com.orlandodiaz.ticketing.catalogo.application.ReservarCupo;
import com.orlandodiaz.ticketing.precios.application.CalcularPrecioDeEntrada;
import com.orlandodiaz.ticketing.shared.application.EjecutorTransaccional;
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
import com.orlandodiaz.ticketing.ventas.infrastructure.persistence.OrdenJpaRepository;
import com.orlandodiaz.ticketing.ventas.infrastructure.persistence.RepositorioJpaDeOrdenes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VentasConfig {

    @Bean
    Reloj reloj() {
        return new RelojDelSistema();
    }

    @Bean
    RepositorioDeOrdenes repositorioDeOrdenes(OrdenJpaRepository jpa) {
        return new RepositorioJpaDeOrdenes(jpa);
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
                                    Reloj reloj, PublicadorDeEventos eventos,
                                    EjecutorTransaccional transaccion) {
        return new ComprarEntradas(precios, cupo, ordenes, reloj, eventos, transaccion);
    }

    @Bean
    CancelarOrden cancelarOrden(RepositorioDeOrdenes ordenes, CupoDeParque cupo, Reloj reloj,
                                PublicadorDeEventos eventos, EjecutorTransaccional transaccion) {
        return new CancelarOrden(ordenes, cupo, reloj, eventos, transaccion);
    }

    @Bean
    ConsultarOrden consultarOrden(RepositorioDeOrdenes ordenes) {
        return new ConsultarOrden(ordenes);
    }
}
