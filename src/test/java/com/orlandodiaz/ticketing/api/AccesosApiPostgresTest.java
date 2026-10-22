package com.orlandodiaz.ticketing.api;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link AccesosApiTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class AccesosApiPostgresTest extends AccesosApiTest {
}
