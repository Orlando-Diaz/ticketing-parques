package com.orlandodiaz.ticketing.persistencia;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link OutboxJpaTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class OutboxPostgresTest extends OutboxJpaTest {
}
