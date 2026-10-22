package com.orlandodiaz.ticketing.persistencia;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link ConcurrenciaCupoJpaTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class ConcurrenciaCupoPostgresTest extends ConcurrenciaCupoJpaTest {
}
