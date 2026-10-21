package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.accesos.application.AnularPaseDeOrden;
import com.orlandodiaz.ticketing.accesos.application.ConsultarPase;
import com.orlandodiaz.ticketing.accesos.application.EmitirPase;
import com.orlandodiaz.ticketing.accesos.application.GeneradorDeCodigos;
import com.orlandodiaz.ticketing.accesos.application.RepositorioDePases;
import com.orlandodiaz.ticketing.accesos.application.Reloj;
import com.orlandodiaz.ticketing.accesos.application.ValidarIngreso;
import com.orlandodiaz.ticketing.accesos.infrastructure.EscuchaDeEventosDeVentas;
import com.orlandodiaz.ticketing.accesos.infrastructure.GeneradorDeCodigosSeguro;
import com.orlandodiaz.ticketing.accesos.infrastructure.persistence.PaseJpaRepository;
import com.orlandodiaz.ticketing.accesos.infrastructure.persistence.RepositorioJpaDePases;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccesosConfig {

    @Bean
    RepositorioDePases repositorioDePases(PaseJpaRepository jpa) {
        return new RepositorioJpaDePases(jpa);
    }

    @Bean
    GeneradorDeCodigos generadorDeCodigos() {
        return new GeneradorDeCodigosSeguro();
    }

    /** "Hoy" en la puerta del parque es la hora de Colombia, sin importar donde corra el servidor. */
    @Bean
    Reloj relojDeAccesos() {
        return () -> LocalDateTime.now(ZoneId.of("America/Bogota"));
    }

    @Bean
    EmitirPase emitirPase(RepositorioDePases pases, GeneradorDeCodigos codigos) {
        return new EmitirPase(pases, codigos);
    }

    @Bean
    AnularPaseDeOrden anularPaseDeOrden(RepositorioDePases pases) {
        return new AnularPaseDeOrden(pases);
    }

    @Bean
    ValidarIngreso validarIngreso(RepositorioDePases pases, Reloj relojDeAccesos) {
        return new ValidarIngreso(pases, relojDeAccesos);
    }

    @Bean
    ConsultarPase consultarPase(RepositorioDePases pases) {
        return new ConsultarPase(pases);
    }

    /** Al crearse, esta escucha se suscribe a los eventos de ventas en el bus. */
    @Bean
    EscuchaDeEventosDeVentas escuchaDeVentasParaAccesos(EmitirPase emitirPase, AnularPaseDeOrden anularPase,
                                                        BusDeEventosEnMemoria bus) {
        var escucha = new EscuchaDeEventosDeVentas(emitirPase, anularPase);
        escucha.registrarEn(bus);
        return escucha;
    }
}
