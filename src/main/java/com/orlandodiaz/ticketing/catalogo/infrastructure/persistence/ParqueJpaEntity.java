package com.orlandodiaz.ticketing.catalogo.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA: como se ve un parque EN LA BASE DE DATOS. Es distinta de la clase Parque del dominio
 * a proposito: el dominio no lleva anotaciones de JPA y puede cambiar sin tocar la tabla (y viceversa).
 */
@Entity
@Table(name = "parque")
public class ParqueJpaEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "cupo_diario", nullable = false)
    private int cupoDiario;

    protected ParqueJpaEntity() {
        // JPA lo exige
    }

    public ParqueJpaEntity(String id, String nombre, int cupoDiario) {
        this.id = id;
        this.nombre = nombre;
        this.cupoDiario = cupoDiario;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCupoDiario() {
        return cupoDiario;
    }
}
