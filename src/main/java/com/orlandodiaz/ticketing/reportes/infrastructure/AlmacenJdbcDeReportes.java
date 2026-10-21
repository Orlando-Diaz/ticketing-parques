package com.orlandodiaz.ticketing.reportes.infrastructure;

import com.orlandodiaz.ticketing.reportes.application.AlmacenDeReportes;
import com.orlandodiaz.ticketing.reportes.domain.IngresoReportado;
import com.orlandodiaz.ticketing.reportes.domain.OrdenReportada;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ADAPTADOR de salida: guarda los hechos de los reportes con SQL directo (JdbcTemplate).
 * Aqui no hay entidades JPA a proposito: es un modelo de LECTURA, sin reglas de negocio que proteger, y el SQL
 * simple es lo mas claro. Cada instruccion se confirma sola (no hace falta una transaccion mas grande).
 *
 * Idempotencia: un hecho repetido choca con la llave primaria (DuplicateKeyException) y se ignora.
 */
public class AlmacenJdbcDeReportes implements AlmacenDeReportes {

    private final JdbcTemplate jdbc;

    public AlmacenJdbcDeReportes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrarOrden(OrdenReportada o) {
        try {
            jdbc.update("""
                    insert into reporte_orden (orden_ref, parque_id, fecha_visita, entradas, total, cancelada, registrada_en)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """,
                    o.ordenRef(), o.parqueId().valor(), o.fechaVisita(), o.entradas(), o.total().pesos(),
                    o.cancelada(), Timestamp.valueOf(o.registradaEn()));
        } catch (DuplicateKeyException yaExiste) {
            // el evento llego dos veces: no pasa nada (y no pisamos un estado "cancelada" ya registrado)
        }
    }

    @Override
    public void marcarOrdenCancelada(String ordenRef) {
        jdbc.update("update reporte_orden set cancelada = true where orden_ref = ?", ordenRef);
    }

    @Override
    public void registrarIngreso(IngresoReportado i) {
        try {
            jdbc.update("""
                    insert into reporte_ingreso (orden_ref, numero, parque_id, fecha_visita, personas, puerta, momento)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """,
                    i.ordenRef(), i.numero(), i.parqueId().valor(), i.fechaVisita(), i.personas(), i.puerta(),
                    Timestamp.valueOf(i.momento()));
        } catch (DuplicateKeyException yaExiste) {
            // el evento llego dos veces: se ignora
        }
    }

    @Override
    public List<OrdenReportada> ordenesEntre(ParqueId parqueId, LocalDate desde, LocalDate hasta) {
        return jdbc.query("""
                select orden_ref, parque_id, fecha_visita, entradas, total, cancelada, registrada_en
                from reporte_orden
                where parque_id = ? and fecha_visita between ? and ?
                order by fecha_visita, orden_ref
                """,
                (rs, fila) -> new OrdenReportada(rs.getString("orden_ref"), new ParqueId(rs.getString("parque_id")),
                        rs.getObject("fecha_visita", LocalDate.class), rs.getInt("entradas"),
                        Dinero.pesos(rs.getLong("total")), rs.getBoolean("cancelada"),
                        rs.getObject("registrada_en", LocalDateTime.class)),
                parqueId.valor(), desde, hasta);
    }

    @Override
    public List<IngresoReportado> ingresosEntre(ParqueId parqueId, LocalDate desde, LocalDate hasta) {
        return jdbc.query("""
                select orden_ref, numero, parque_id, fecha_visita, personas, puerta, momento
                from reporte_ingreso
                where parque_id = ? and fecha_visita between ? and ?
                order by fecha_visita, orden_ref, numero
                """,
                (rs, fila) -> new IngresoReportado(rs.getString("orden_ref"), rs.getInt("numero"),
                        new ParqueId(rs.getString("parque_id")), rs.getObject("fecha_visita", LocalDate.class),
                        rs.getInt("personas"), rs.getString("puerta"), rs.getObject("momento", LocalDateTime.class)),
                parqueId.valor(), desde, hasta);
    }
}
