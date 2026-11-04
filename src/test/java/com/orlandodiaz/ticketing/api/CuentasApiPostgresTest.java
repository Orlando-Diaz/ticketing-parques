package com.orlandodiaz.ticketing.api;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link CuentasApiTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class CuentasApiPostgresTest extends CuentasApiTest {
}
