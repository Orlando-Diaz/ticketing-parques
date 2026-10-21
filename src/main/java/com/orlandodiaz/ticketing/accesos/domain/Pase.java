package com.orlandodiaz.ticketing.accesos.domain;

import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * El pase de una compra: un solo codigo (el del QR) que sirve para entrar a las N personas de la orden,
 * todas juntas o en varias pasadas. Las reglas de la puerta viven aqui adentro:
 *  - solo vale el dia de la visita;
 *  - no vale si la compra se cancelo;
 *  - nunca entran mas personas que las compradas.
 *
 * La {@code version} es el numero de edicion leido del almacen (null = pase nuevo, aun sin guardar). El dominio
 * no la usa para ninguna regla: solo la lleva de ida y vuelta para detectar dos puertas escaneando el mismo
 * QR a la vez (ver ADR 0004).
 *
 * Igual que Orden, el pase REGISTRA eventos (IngresoRegistrado) pero no los publica: el caso de uso los recoge
 * con sacarEventos() y los publica despues de guardar.
 */
public class Pase {

    private final String id;
    private final String codigo;
    private final String ordenRef;
    private final ParqueId parqueId;
    private final LocalDate fechaVisita;
    private final int entradasTotales;
    private final LocalDateTime emitidoEn;
    private final List<Ingreso> ingresos;
    private final Long version;
    private final List<EventoDeDominio> eventos = new ArrayList<>();
    private EstadoPase estado;

    private Pase(String id, String codigo, String ordenRef, ParqueId parqueId, LocalDate fechaVisita,
                 int entradasTotales, LocalDateTime emitidoEn, EstadoPase estado, List<Ingreso> ingresos,
                 Long version) {
        this.id = Objects.requireNonNull(id, "El id es obligatorio");
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El codigo del pase es obligatorio");
        }
        this.codigo = codigo;
        if (ordenRef == null || ordenRef.isBlank()) {
            throw new IllegalArgumentException("El pase necesita la orden a la que pertenece");
        }
        this.ordenRef = ordenRef;
        this.parqueId = Objects.requireNonNull(parqueId, "El parque es obligatorio");
        this.fechaVisita = Objects.requireNonNull(fechaVisita, "La fecha de visita es obligatoria");
        if (entradasTotales < 1) {
            throw new IllegalArgumentException("El pase necesita al menos una entrada");
        }
        this.entradasTotales = entradasTotales;
        this.emitidoEn = Objects.requireNonNull(emitidoEn, "La fecha de emision es obligatoria");
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        this.ingresos = new ArrayList<>(ingresos);
        if (entradasUsadas() > entradasTotales) {
            throw new IllegalArgumentException("Los ingresos superan las entradas del pase");
        }
        this.version = version;
    }

    public static Pase emitir(String codigo, String ordenRef, ParqueId parqueId, LocalDate fechaVisita,
                              int entradas, LocalDateTime emitidoEn) {
        return new Pase(UUID.randomUUID().toString(), codigo, ordenRef, parqueId, fechaVisita, entradas,
                emitidoEn, EstadoPase.ACTIVO, List.of(), null);
    }

    /** Para que la persistencia vuelva a armar un pase ya guardado. */
    public static Pase reconstruir(String id, String codigo, String ordenRef, ParqueId parqueId,
                                   LocalDate fechaVisita, int entradasTotales, LocalDateTime emitidoEn,
                                   EstadoPase estado, List<Ingreso> ingresos, long version) {
        return new Pase(id, codigo, ordenRef, parqueId, fechaVisita, entradasTotales, emitidoEn, estado,
                ingresos, version);
    }

    /** Deja entrar a {@code personas} con este pase. {@code ahora} decide que dia es "hoy". */
    public void registrarIngreso(int personas, String puerta, LocalDateTime ahora) {
        if (personas < 1) {
            throw new IllegalArgumentException("Entra al menos una persona");
        }
        if (estado == EstadoPase.ANULADO) {
            throw new PaseAnuladoException();
        }
        if (!fechaVisita.equals(ahora.toLocalDate())) {
            throw new FechaDePaseIncorrectaException(fechaVisita, ahora.toLocalDate());
        }
        if (personas > entradasRestantes()) {
            throw new EntradasDelPaseAgotadasException(personas, entradasRestantes());
        }
        Ingreso ingreso = new Ingreso(personas, puerta, ahora);
        ingresos.add(ingreso);
        eventos.add(new IngresoRegistrado(ordenRef, id, parqueId, fechaVisita, ingresos.size() - 1,
                ingreso.personas(), ingreso.puerta(), ahora));
    }

    /** Entrega los eventos pendientes de publicar y los borra, para no publicarlos dos veces. */
    public List<EventoDeDominio> sacarEventos() {
        List<EventoDeDominio> pendientes = List.copyOf(eventos);
        eventos.clear();
        return pendientes;
    }

    /** Anula el pase (la compra se cancelo). Anular uno ya anulado no hace nada. */
    public void anular() {
        this.estado = EstadoPase.ANULADO;
    }

    public int entradasUsadas() {
        return ingresos.stream().mapToInt(Ingreso::personas).sum();
    }

    public int entradasRestantes() {
        return entradasTotales - entradasUsadas();
    }

    public String id() {
        return id;
    }

    public String codigo() {
        return codigo;
    }

    public String ordenRef() {
        return ordenRef;
    }

    public ParqueId parqueId() {
        return parqueId;
    }

    public LocalDate fechaVisita() {
        return fechaVisita;
    }

    public int entradasTotales() {
        return entradasTotales;
    }

    public LocalDateTime emitidoEn() {
        return emitidoEn;
    }

    public EstadoPase estado() {
        return estado;
    }

    public List<Ingreso> ingresos() {
        return List.copyOf(ingresos);
    }

    /** Edicion leida del almacen; null si el pase es nuevo y aun no se guardo. */
    public Long version() {
        return version;
    }
}
