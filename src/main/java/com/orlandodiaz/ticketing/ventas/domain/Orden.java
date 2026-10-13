package com.orlandodiaz.ticketing.ventas.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Una compra de entradas para un parque y una fecha. Se crea CONFIRMADA y puede cancelarse una vez.
 * Las reglas de la orden (fecha valida, minimo una entrada, no cancelar dos veces) viven aqui.
 */
public class Orden {

    private final OrdenId id;
    private final ParqueId parqueId;
    private final LocalDate fechaVisita;
    private final List<LineaDeOrden> lineas;
    private final LocalDateTime creadaEn;
    private final Dinero total;
    private EstadoOrden estado;

    private Orden(ParqueId parqueId, LocalDate fechaVisita, List<LineaDeOrden> lineas, LocalDateTime creadaEn) {
        this.id = OrdenId.nuevo();
        this.parqueId = Objects.requireNonNull(parqueId, "El parque es obligatorio");
        this.fechaVisita = Objects.requireNonNull(fechaVisita, "La fecha de visita es obligatoria");
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("La orden necesita al menos una entrada");
        }
        this.lineas = List.copyOf(lineas);
        this.creadaEn = Objects.requireNonNull(creadaEn, "La fecha de creacion es obligatoria");
        this.total = this.lineas.stream().map(LineaDeOrden::precio).reduce(Dinero.CERO, Dinero::sumar);
        this.estado = EstadoOrden.CONFIRMADA;
    }

    public static Orden crear(ParqueId parqueId, LocalDate fechaVisita, List<LineaDeOrden> lineas, LocalDateTime creadaEn) {
        exigirFechaValida(fechaVisita, creadaEn.toLocalDate());
        return new Orden(parqueId, fechaVisita, lineas, creadaEn);
    }

    /** No se puede comprar para un dia que ya paso (hoy si se puede). */
    public static void exigirFechaValida(LocalDate fechaVisita, LocalDate hoy) {
        if (fechaVisita.isBefore(hoy)) {
            throw new FechaDeVisitaInvalidaException(fechaVisita, hoy);
        }
    }

    public void cancelar(LocalDate hoy) {
        if (estado == EstadoOrden.CANCELADA) {
            throw new OrdenNoCancelableException("La orden ya estaba cancelada");
        }
        if (fechaVisita.isBefore(hoy)) {
            throw new OrdenNoCancelableException("No se puede cancelar una orden cuya fecha de visita ya paso");
        }
        this.estado = EstadoOrden.CANCELADA;
    }

    public int cantidadEntradas() {
        return lineas.size();
    }

    public OrdenId id() {
        return id;
    }

    public ParqueId parqueId() {
        return parqueId;
    }

    public LocalDate fechaVisita() {
        return fechaVisita;
    }

    public List<LineaDeOrden> lineas() {
        return lineas;
    }

    public LocalDateTime creadaEn() {
        return creadaEn;
    }

    public Dinero total() {
        return total;
    }

    public EstadoOrden estado() {
        return estado;
    }
}
