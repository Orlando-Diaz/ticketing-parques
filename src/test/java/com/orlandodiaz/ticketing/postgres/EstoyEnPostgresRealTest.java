package com.orlandodiaz.ticketing.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Comprueba que el montaje funciona: es PostgreSQL de verdad y todas las migraciones se aplicaron. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ConPostgresReal
class EstoyEnPostgresRealTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void laBaseEsPostgreSqlYLasMigracionesCorrieron() {
        String version = jdbc.queryForObject("select version()", String.class);
        Integer migraciones = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);

        assertThat(version).startsWith("PostgreSQL");
        assertThat(migraciones).as("V1 a V7 aplicadas").isGreaterThanOrEqualTo(7);
    }
}
