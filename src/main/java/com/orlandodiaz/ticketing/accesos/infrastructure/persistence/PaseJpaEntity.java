package com.orlandodiaz.ticketing.accesos.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pase")
public class PaseJpaEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Column(name = "orden_ref", nullable = false)
    private String ordenRef;

    @Column(name = "parque_id", nullable = false)
    private String parqueId;

    @Column(name = "fecha_visita", nullable = false)
    private LocalDate fechaVisita;

    @Column(name = "entradas_totales", nullable = false)
    private int entradasTotales;

    /** Se guarda aparte del historial para que cada ingreso cambie una columna de ESTA fila (y suba la version). */
    @Column(name = "entradas_usadas", nullable = false)
    private int entradasUsadas;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "emitido_en", nullable = false)
    private LocalDateTime emitidoEn;

    /** Bloqueo optimista: Hibernate lo sube en cada UPDATE. Long (no long): null = fila nueva. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pase_id", nullable = false)
    @OrderBy("posicion ASC")
    private List<IngresoJpaEntity> ingresos = new ArrayList<>();

    protected PaseJpaEntity() {
    }

    public PaseJpaEntity(String id, String codigo, String ordenRef, String parqueId, LocalDate fechaVisita,
                         int entradasTotales, int entradasUsadas, String estado, LocalDateTime emitidoEn) {
        this.id = id;
        this.codigo = codigo;
        this.ordenRef = ordenRef;
        this.parqueId = parqueId;
        this.fechaVisita = fechaVisita;
        this.entradasTotales = entradasTotales;
        this.entradasUsadas = entradasUsadas;
        this.estado = estado;
        this.emitidoEn = emitidoEn;
    }

    public void actualizar(String nuevoEstado, int nuevasEntradasUsadas) {
        this.estado = nuevoEstado;
        this.entradasUsadas = nuevasEntradasUsadas;
    }

    public void agregarIngreso(IngresoJpaEntity ingreso) {
        ingresos.add(ingreso);
    }

    public String getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getOrdenRef() {
        return ordenRef;
    }

    public String getParqueId() {
        return parqueId;
    }

    public LocalDate getFechaVisita() {
        return fechaVisita;
    }

    public int getEntradasTotales() {
        return entradasTotales;
    }

    public int getEntradasUsadas() {
        return entradasUsadas;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getEmitidoEn() {
        return emitidoEn;
    }

    public Long getVersion() {
        return version;
    }

    public List<IngresoJpaEntity> getIngresos() {
        return ingresos;
    }
}
