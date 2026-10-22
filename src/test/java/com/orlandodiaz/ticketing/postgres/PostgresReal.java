package com.orlandodiaz.ticketing.postgres;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Un PostgreSQL REAL (en Docker) compartido por todos los tests que lo pidan con {@link ConPostgresReal}.
 * Se levanta una sola vez por ejecucion y Testcontainers lo apaga al terminar. Las migraciones de Flyway corren
 * contra el: asi se prueba lo mismo que en produccion (bloqueo optimista, restricciones CHECK, SQL del outbox...).
 */
public final class PostgresReal {

    private static final String IMAGEN = "postgres:18-alpine"; // la misma familia que usamos en desarrollo
    private static GenericContainer<?> contenedor;

    private PostgresReal() {
    }

    /** Lo consulta @EnabledIf: sin Docker los tests de PostgreSQL se omiten en vez de fallar. */
    public static boolean dockerDisponible() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static synchronized GenericContainer<?> contenedor() {
        if (contenedor == null) {
            contenedor = new GenericContainer<>(DockerImageName.parse(IMAGEN))
                    .withEnv("POSTGRES_DB", "ticketing")
                    .withEnv("POSTGRES_PASSWORD", "solo-para-tests")
                    .withExposedPorts(5432)
                    // PostgreSQL imprime este mensaje dos veces: una al iniciar el script y otra cuando ya acepta conexiones
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));
            contenedor.start();
        }
        return contenedor;
    }

    /** Apunta la aplicacion al contenedor (pisa la URL de H2 del perfil "test"). */
    public static class Inicializador implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext contexto) {
            GenericContainer<?> pg = contenedor();
            TestPropertyValues.of(
                    "spring.datasource.url=jdbc:postgresql://" + pg.getHost() + ":" + pg.getMappedPort(5432) + "/ticketing",
                    "spring.datasource.username=postgres",
                    "spring.datasource.password=solo-para-tests"
            ).applyTo(contexto);
        }
    }
}
