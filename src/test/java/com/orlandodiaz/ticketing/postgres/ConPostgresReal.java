package com.orlandodiaz.ticketing.postgres;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.context.ContextConfiguration;

/** Marca un test para que corra contra PostgreSQL real (Docker). Se omite solo si no hay Docker. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ContextConfiguration(initializers = PostgresReal.Inicializador.class)
@EnabledIf("com.orlandodiaz.ticketing.postgres.PostgresReal#dockerDisponible")
public @interface ConPostgresReal {
}
