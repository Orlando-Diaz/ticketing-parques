package com.orlandodiaz.ticketing.facturacion.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un contador por nombre (hoy solo "FACTURA"). Sirve para numerar las facturas sin saltos. */
@Entity
@Table(name = "consecutivo")
public class ConsecutivoJpaEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "ultimo", nullable = false)
    private long ultimo;

    protected ConsecutivoJpaEntity() {
    }

    public String getId() {
        return id;
    }

    public long getUltimo() {
        return ultimo;
    }
}
