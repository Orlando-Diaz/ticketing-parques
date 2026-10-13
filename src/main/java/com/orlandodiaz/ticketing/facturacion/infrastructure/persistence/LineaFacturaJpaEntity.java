package com.orlandodiaz.ticketing.facturacion.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "linea_factura")
public class LineaFacturaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posicion", nullable = false)
    private int posicion;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "valor", nullable = false)
    private long valor;

    protected LineaFacturaJpaEntity() {
    }

    public LineaFacturaJpaEntity(int posicion, String descripcion, long valor) {
        this.posicion = posicion;
        this.descripcion = descripcion;
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public long getValor() {
        return valor;
    }
}
