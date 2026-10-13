package com.orlandodiaz.ticketing.facturacion.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Factura de una orden. Facturacion NO conoce las clases de ventas: guarda solo una referencia
 * (el texto del id de la orden) y sus propias lineas. Asi los dos modulos quedan desacoplados.
 */
public class Factura {

    private final String id;
    private final String numero;
    private final String ordenRef;
    private final ParqueId parqueId;
    private final List<LineaDeFactura> lineas;
    private final Dinero total;
    private final LocalDateTime emitidaEn;
    private EstadoFactura estado;

    private Factura(String id, String numero, String ordenRef, ParqueId parqueId, List<LineaDeFactura> lineas,
                    LocalDateTime emitidaEn, EstadoFactura estado) {
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("La factura necesita un numero");
        }
        if (ordenRef == null || ordenRef.isBlank()) {
            throw new IllegalArgumentException("La factura debe referenciar una orden");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("La factura necesita al menos una linea");
        }
        this.id = Objects.requireNonNull(id, "El id es obligatorio");
        this.numero = numero;
        this.ordenRef = ordenRef;
        this.parqueId = Objects.requireNonNull(parqueId, "El parque es obligatorio");
        this.lineas = List.copyOf(lineas);
        this.emitidaEn = Objects.requireNonNull(emitidaEn, "La fecha de emision es obligatoria");
        this.total = this.lineas.stream().map(LineaDeFactura::valor).reduce(Dinero.CERO, Dinero::sumar);
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
    }

    public static Factura emitir(String numero, String ordenRef, ParqueId parqueId,
                                 List<LineaDeFactura> lineas, LocalDateTime emitidaEn) {
        return new Factura(UUID.randomUUID().toString(), numero, ordenRef, parqueId, lineas, emitidaEn,
                EstadoFactura.EMITIDA);
    }

    /** Para que la persistencia vuelva a armar una factura ya guardada. */
    public static Factura reconstruir(String id, String numero, String ordenRef, ParqueId parqueId,
                                      List<LineaDeFactura> lineas, LocalDateTime emitidaEn, EstadoFactura estado) {
        return new Factura(id, numero, ordenRef, parqueId, lineas, emitidaEn, estado);
    }

    public void anular() {
        if (estado == EstadoFactura.ANULADA) {
            throw new IllegalStateException("La factura " + numero + " ya estaba anulada");
        }
        this.estado = EstadoFactura.ANULADA;
    }

    public String id() {
        return id;
    }

    public String numero() {
        return numero;
    }

    public String ordenRef() {
        return ordenRef;
    }

    public ParqueId parqueId() {
        return parqueId;
    }

    public List<LineaDeFactura> lineas() {
        return lineas;
    }

    public Dinero total() {
        return total;
    }

    public LocalDateTime emitidaEn() {
        return emitidaEn;
    }

    public EstadoFactura estado() {
        return estado;
    }
}
