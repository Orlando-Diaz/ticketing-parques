package com.orlandodiaz.ticketing.api;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link ReportesApiTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class ReportesApiPostgresTest extends ReportesApiTest {
}
