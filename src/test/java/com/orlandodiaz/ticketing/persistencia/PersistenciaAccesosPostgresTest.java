package com.orlandodiaz.ticketing.persistencia;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link PersistenciaAccesosJpaTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class PersistenciaAccesosPostgresTest extends PersistenciaAccesosJpaTest {
}
