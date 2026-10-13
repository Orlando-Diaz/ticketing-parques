package com.orlandodiaz.ticketing.facturacion.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "factura")
public class FacturaJpaEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "numero", nullable = false)
    private String numero;

    @Column(name = "orden_ref", nullable = false)
    private String ordenRef;

    @Column(name = "parque_id", nullable = false)
    private String parqueId;

    @Column(name = "total", nullable = false)
    private long total;

    @Column(name = "emitida_en", nullable = false)
    private LocalDateTime emitidaEn;

    @Column(name = "estado", nullable = false)
    private String estado;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "factura_id", nullable = false)
    @OrderBy("posicion ASC")
    private List<LineaFacturaJpaEntity> lineas = new ArrayList<>();

    protected FacturaJpaEntity() {
    }

    public FacturaJpaEntity(String id, String numero, String ordenRef, String parqueId, long total,
                            LocalDateTime emitidaEn, String estado) {
        this.id = id;
        this.numero = numero;
        this.ordenRef = ordenRef;
        this.parqueId = parqueId;
        this.total = total;
        this.emitidaEn = emitidaEn;
        this.estado = estado;
    }

    public void agregarLinea(LineaFacturaJpaEntity linea) {
        lineas.add(linea);
    }

    public void cambiarEstado(String nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public String getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public String getOrdenRef() {
        return ordenRef;
    }

    public String getParqueId() {
        return parqueId;
    }

    public LocalDateTime getEmitidaEn() {
        return emitidaEn;
    }

    public String getEstado() {
        return estado;
    }

    public List<LineaFacturaJpaEntity> getLineas() {
        return lineas;
    }
}
