package com.orlandodiaz.ticketing.persistencia;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link PersistenciaJpaTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class PersistenciaPostgresTest extends PersistenciaJpaTest {
}
