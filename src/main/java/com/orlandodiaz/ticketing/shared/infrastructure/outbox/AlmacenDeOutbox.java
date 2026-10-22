package com.orlandodiaz.ticketing.shared.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La tabla {@code outbox}. Usa JdbcTemplate, que participa en la MISMA transaccion que JPA (comparten conexion):
 * por eso {@link #agregar} dentro de la transaccion de un caso de uso se confirma o se deshace junto con el agregado.
 * Estados de una fila: pendiente (publicado_en nulo, fallido=false), publicada, o fallida (se rindio tras N intentos).
 */
public class AlmacenDeOutbox {

    private static final int LARGO_MAXIMO_DEL_ERROR = 500;

    private final JdbcTemplate jdbc;

    public AlmacenDeOutbox(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void agregar(String tipo, String payload, LocalDateTime creadoEn) {
        jdbc.update("insert into outbox (tipo, payload, creado_en) values (?, ?, ?)", tipo, payload, creadoEn);
    }

    /** Pendientes con id mayor a {@code despuesDeId}, del mas viejo al mas nuevo (se respeta el orden de llegada). */
    public List<MensajeDeOutbox> pendientes(long despuesDeId, int limite) {
        return jdbc.query(
                "select id, tipo, payload, intentos from outbox"
                        + " where publicado_en is null and fallido = false and id > ?"
                        + " order by id fetch first " + limite + " rows only",
                (rs, fila) -> new MensajeDeOutbox(rs.getLong("id"), rs.getString("tipo"),
                        rs.getString("payload"), rs.getInt("intentos")),
                despuesDeId);
    }

    public void marcarPublicado(long id, LocalDateTime cuando) {
        jdbc.update("update outbox set publicado_en = ?, ultimo_error = null where id = ?", cuando, id);
    }

    /** Anota un intento fallido; si ya van {@code maximoDeIntentos}, la fila pasa a fallida y deja de reintentarse. */
    public void registrarFallo(long id, String error, int maximoDeIntentos) {
        String recortado = error == null ? "" : error.substring(0, Math.min(error.length(), LARGO_MAXIMO_DEL_ERROR));
        jdbc.update("update outbox set intentos = intentos + 1, ultimo_error = ?,"
                + " fallido = (intentos + 1 >= ?) where id = ?", recortado, maximoDeIntentos, id);
    }

    public long contarPendientes() {
        return jdbc.queryForObject(
                "select count(*) from outbox where publicado_en is null and fallido = false", Long.class);
    }

    public long contarFallidos() {
        return jdbc.queryForObject("select count(*) from outbox where fallido = true", Long.class);
    }
}
