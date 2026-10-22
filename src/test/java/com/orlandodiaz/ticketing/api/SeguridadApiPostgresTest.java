package com.orlandodiaz.ticketing.api;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link SeguridadApiTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class SeguridadApiPostgresTest extends SeguridadApiTest {
}
