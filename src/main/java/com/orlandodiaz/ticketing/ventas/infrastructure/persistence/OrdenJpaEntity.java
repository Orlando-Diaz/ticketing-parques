package com.orlandodiaz.ticketing.ventas.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden")
public class OrdenJpaEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "parque_id", nullable = false)
    private String parqueId;

    @Column(name = "fecha_visita", nullable = false)
    private LocalDate fechaVisita;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "total", nullable = false)
    private long total;

    @Column(name = "estado", nullable = false)
    private String estado;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "orden_id", nullable = false)
    @OrderBy("posicion ASC")
    private List<LineaOrdenJpaEntity> lineas = new ArrayList<>();

    protected OrdenJpaEntity() {
    }

    public OrdenJpaEntity(String id, String parqueId, LocalDate fechaVisita, LocalDateTime creadaEn,
                          long total, String estado) {
        this.id = id;
        this.parqueId = parqueId;
        this.fechaVisita = fechaVisita;
        this.creadaEn = creadaEn;
        this.total = total;
        this.estado = estado;
    }

    public void agregarLinea(LineaOrdenJpaEntity linea) {
        lineas.add(linea);
    }

    public void cambiarEstado(String nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public String getId() {
        return id;
    }

    public String getParqueId() {
        return parqueId;
    }

    public LocalDate getFechaVisita() {
        return fechaVisita;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public String getEstado() {
        return estado;
    }

    public List<LineaOrdenJpaEntity> getLineas() {
        return lineas;
    }
}
