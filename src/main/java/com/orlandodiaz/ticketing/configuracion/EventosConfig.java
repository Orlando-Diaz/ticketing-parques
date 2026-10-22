package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.accesos.domain.IngresoRegistrado;
import com.orlandodiaz.ticketing.shared.application.EjecutorTransaccional;
import com.orlandodiaz.ticketing.shared.application.PublicadorDeEventos;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import com.orlandodiaz.ticketing.shared.infrastructure.outbox.AlmacenDeOutbox;
import com.orlandodiaz.ticketing.shared.infrastructure.outbox.DespachadorDeOutbox;
import com.orlandodiaz.ticketing.shared.infrastructure.outbox.EjecutorTransaccionalSpring;
import com.orlandodiaz.ticketing.shared.infrastructure.outbox.ProgramadorDelOutbox;
import com.orlandodiaz.ticketing.shared.infrastructure.outbox.PublicadorConOutbox;
import com.orlandodiaz.ticketing.shared.infrastructure.outbox.SerializadorDeEventos;
import com.orlandodiaz.ticketing.ventas.domain.OrdenCancelada;
import com.orlandodiaz.ticketing.ventas.domain.OrdenConfirmada;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * RAIZ DE COMPOSICION: las clases del dominio y de application no tienen anotaciones de Spring.
 * Aqui (y en las otras clases *Config) es donde le decimos a Spring como construirlas y conectarlas.
 *
 * Eventos: quien publica (casos de uso) anota el evento en el OUTBOX dentro de su transaccion; el despachador lo
 * lee y lo entrega al BUS en memoria, donde estan suscritos los modulos. Ver ADR 0007.
 */
@Configuration
@EnableScheduling
public class EventosConfig {

    @Bean
    BusDeEventosEnMemoria busDeEventos() {
        return new BusDeEventosEnMemoria();
    }

    /** Aqui se declaran los eventos que pueden viajar por el outbox. Un evento nuevo se registra en esta lista. */
    @Bean
    SerializadorDeEventos serializadorDeEventos() {
        return new SerializadorDeEventos()
                .registrar(OrdenConfirmada.class)
                .registrar(OrdenCancelada.class)
                .registrar(IngresoRegistrado.class);
    }

    @Bean
    AlmacenDeOutbox almacenDeOutbox(JdbcTemplate jdbc) {
        return new AlmacenDeOutbox(jdbc);
    }

    /** El bus tambien es un PublicadorDeEventos, pero quien publica debe usar el outbox: este es el principal. */
    @Bean
    @Primary
    PublicadorDeEventos publicadorDeEventos(AlmacenDeOutbox outbox, SerializadorDeEventos serializador) {
        return new PublicadorConOutbox(outbox, serializador);
    }

    @Bean
    DespachadorDeOutbox despachadorDeOutbox(AlmacenDeOutbox outbox, SerializadorDeEventos serializador,
                                            BusDeEventosEnMemoria bus,
                                            @Value("${ticketing.outbox.maximo-de-intentos:10}") int maximoDeIntentos) {
        return new DespachadorDeOutbox(outbox, serializador, bus, maximoDeIntentos);
    }

    @Bean
    EjecutorTransaccional ejecutorTransaccional(PlatformTransactionManager manager, DespachadorDeOutbox despachador) {
        return new EjecutorTransaccionalSpring(manager, despachador);
    }

    /** Se puede apagar con ticketing.outbox.programador-activo=false (los tests lo apagan para controlar el momento). */
    @Bean
    @ConditionalOnProperty(name = "ticketing.outbox.programador-activo", havingValue = "true", matchIfMissing = true)
    ProgramadorDelOutbox programadorDelOutbox(DespachadorDeOutbox despachador) {
        return new ProgramadorDelOutbox(despachador);
    }
}
