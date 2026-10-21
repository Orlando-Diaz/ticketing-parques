package com.orlandodiaz.ticketing.ventas.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Una compra de entradas para un parque y una fecha. Se crea CONFIRMADA y puede cancelarse una vez.
 * Las reglas de la orden (fecha valida, minimo una entrada, no cancelar dos veces) viven aqui.
 *
 * Cuando algo importante pasa, la orden REGISTRA un evento. No lo publica ella (no conoce el bus):
 * el caso de uso lo recoge con sacarEventos() y lo publica despues de guardar.
 *
 * La {@code version} es el "numero de edicion" leido del almacen (null = orden nueva, aun sin guardar).
 * El dominio no la usa para ninguna regla: solo la lleva de ida y vuelta para que el adaptador detecte si
 * alguien mas modifico esta orden mientras tanto (ver ADR 0004).
 */
public class Orden {

    private final OrdenId id;
    private final ParqueId parqueId;
    private final LocalDate fechaVisita;
    private final List<LineaDeOrden> lineas;
    private final LocalDateTime creadaEn;
    private final Dinero total;
    private EstadoOrden estado;
    private final Long version;
    private final List<EventoDeDominio> eventos = new ArrayList<>();

    private Orden(OrdenId id, ParqueId parqueId, LocalDate fechaVisita, List<LineaDeOrden> lineas,
                  LocalDateTime creadaEn, EstadoOrden estado, Long version) {
        this.id = Objects.requireNonNull(id, "El id es obligatorio");
        this.parqueId = Objects.requireNonNull(parqueId, "El parque es obligatorio");
        this.fechaVisita = Objects.requireNonNull(fechaVisita, "La fecha de visita es obligatoria");
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("La orden necesita al menos una entrada");
        }
        this.lineas = List.copyOf(lineas);
        this.creadaEn = Objects.requireNonNull(creadaEn, "La fecha de creacion es obligatoria");
        this.total = this.lineas.stream().map(LineaDeOrden::precio).reduce(Dinero.CERO, Dinero::sumar);
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        this.version = version;
    }

    public static Orden crear(ParqueId parqueId, LocalDate fechaVisita, List<LineaDeOrden> lineas, LocalDateTime creadaEn) {
        exigirFechaValida(fechaVisita, creadaEn.toLocalDate());
        Orden orden = new Orden(OrdenId.nuevo(), parqueId, fechaVisita, lineas, creadaEn, EstadoOrden.CONFIRMADA, null);
        orden.eventos.add(new OrdenConfirmada(
                orden.id, orden.parqueId, orden.fechaVisita, orden.lineas, orden.total, creadaEn));
        return orden;
    }

    /**
     * Para que la persistencia vuelva a armar una orden ya guardada. No valida la fecha (es historia)
     * ni registra eventos: no esta ocurriendo nada nuevo, solo se esta leyendo.
     */
    public static Orden reconstruir(OrdenId id, ParqueId parqueId, LocalDate fechaVisita,
                                    List<LineaDeOrden> lineas, LocalDateTime creadaEn, EstadoOrden estado,
                                    long version) {
        return new Orden(id, parqueId, fechaVisita, lineas, creadaEn, estado, version);
    }

    /** No se puede comprar para un dia que ya paso (hoy si se puede). */
    public static void exigirFechaValida(LocalDate fechaVisita, LocalDate hoy) {
        if (fechaVisita.isBefore(hoy)) {
            throw new FechaDeVisitaInvalidaException(fechaVisita, hoy);
        }
    }

    public void cancelar(LocalDateTime ahora) {
        if (estado == EstadoOrden.CANCELADA) {
            throw new OrdenNoCancelableException("La orden ya estaba cancelada");
        }
        if (fechaVisita.isBefore(ahora.toLocalDate())) {
            throw new OrdenNoCancelableException("No se puede cancelar una orden cuya fecha de visita ya paso");
        }
        this.estado = EstadoOrden.CANCELADA;
        eventos.add(new OrdenCancelada(id, parqueId, fechaVisita, total, ahora));
    }

    /** Entrega los eventos pendientes de publicar y los borra, para no publicarlos dos veces. */
    public List<EventoDeDominio> sacarEventos() {
        List<EventoDeDominio> pendientes = List.copyOf(eventos);
        eventos.clear();
        return pendientes;
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

    /** Edicion leida del almacen; null si la orden es nueva y aun no se guardo. */
    public Long version() {
        return version;
    }
}
