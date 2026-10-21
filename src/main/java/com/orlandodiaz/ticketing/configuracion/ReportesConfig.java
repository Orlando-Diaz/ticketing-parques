package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.reportes.application.ActualizarReportes;
import com.orlandodiaz.ticketing.reportes.application.AlmacenDeReportes;
import com.orlandodiaz.ticketing.reportes.application.ConsultarReportes;
import com.orlandodiaz.ticketing.reportes.infrastructure.AlmacenJdbcDeReportes;
import com.orlandodiaz.ticketing.reportes.infrastructure.EscuchaDeEventosDeOtrosModulos;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ReportesConfig {

    @Bean
    AlmacenDeReportes almacenDeReportes(JdbcTemplate jdbc) {
        return new AlmacenJdbcDeReportes(jdbc);
    }

    @Bean
    ActualizarReportes actualizarReportes(AlmacenDeReportes almacen) {
        return new ActualizarReportes(almacen);
    }

    @Bean
    ConsultarReportes consultarReportes(AlmacenDeReportes almacen) {
        return new ConsultarReportes(almacen);
    }

    /** Al crearse, esta escucha se suscribe a los eventos de ventas y accesos en el bus. */
    @Bean
    EscuchaDeEventosDeOtrosModulos escuchaDeEventosParaReportes(ActualizarReportes actualizar,
                                                               BusDeEventosEnMemoria bus) {
        var escucha = new EscuchaDeEventosDeOtrosModulos(actualizar);
        escucha.registrarEn(bus);
        return escucha;
    }
}
