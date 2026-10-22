package com.orlandodiaz.ticketing.persistencia;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link ConcurrenciaOrdenJpaTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class ConcurrenciaOrdenPostgresTest extends ConcurrenciaOrdenJpaTest {
}
