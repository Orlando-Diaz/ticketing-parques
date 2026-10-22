package com.orlandodiaz.ticketing.persistencia;

import com.orlandodiaz.ticketing.postgres.ConPostgresReal;

/** Los mismos tests de {@link PersistenciaReportesTest}, pero contra PostgreSQL real en Docker. */
@ConPostgresReal
class PersistenciaReportesPostgresTest extends PersistenciaReportesTest {
}
