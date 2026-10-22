package com.orlandodiaz.ticketing.api;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link TicketingApiTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class TicketingApiPostgresTest extends TicketingApiTest {
}
