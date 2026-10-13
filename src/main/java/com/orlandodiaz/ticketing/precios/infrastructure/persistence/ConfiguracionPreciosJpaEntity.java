package com.orlandodiaz.ticketing.precios.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "configuracion_precios")
public class ConfiguracionPreciosJpaEntity {

    @Id
    @Column(name = "parque_id")
    private String parqueId;

    @Column(name = "esquema_tipo", nullable = false)
    private String esquemaTipo;

    @Column(name = "precio_unico")
    private Long precioUnico;

    // Los hijos viven y mueren con la configuracion (cascade + orphanRemoval).
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "parque_id", nullable = false)
    @OrderBy("posicion ASC")
    private List<CategoriaTarifaJpaEntity> categorias = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "parque_id", nullable = false)
    @OrderBy("posicion ASC")
    private List<ReglaPrecioJpaEntity> reglas = new ArrayList<>();

    protected ConfiguracionPreciosJpaEntity() {
    }

    public ConfiguracionPreciosJpaEntity(String parqueId) {
        this.parqueId = parqueId;
    }

    /** Borra el contenido para volver a llenarlo (la configuracion se reemplaza completa). */
    public void limpiar() {
        categorias.clear();
        reglas.clear();
        precioUnico = null;
    }

    public void definirUnica(long precio) {
        this.esquemaTipo = "UNICA";
        this.precioUnico = precio;
    }

    public void definirPorCategoria() {
        this.esquemaTipo = "POR_CATEGORIA";
    }

    public void agregarCategoria(CategoriaTarifaJpaEntity categoria) {
        categorias.add(categoria);
    }

    public void agregarRegla(ReglaPrecioJpaEntity regla) {
        reglas.add(regla);
    }

    public String getParqueId() {
        return parqueId;
    }

    public String getEsquemaTipo() {
        return esquemaTipo;
    }

    public Long getPrecioUnico() {
        return precioUnico;
    }

    public List<CategoriaTarifaJpaEntity> getCategorias() {
        return categorias;
    }

    public List<ReglaPrecioJpaEntity> getReglas() {
        return reglas;
    }
}
