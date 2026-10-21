package com.orlandodiaz.ticketing.accesos.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingreso")
public class IngresoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posicion", nullable = false)
    private int posicion;

    @Column(name = "personas", nullable = false)
    private int personas;

    @Column(name = "puerta", nullable = false)
    private String puerta;

    @Column(name = "momento", nullable = false)
    private LocalDateTime momento;

    protected IngresoJpaEntity() {
    }

    public IngresoJpaEntity(int posicion, int personas, String puerta, LocalDateTime momento) {
        this.posicion = posicion;
        this.personas = personas;
        this.puerta = puerta;
        this.momento = momento;
    }

    public int getPersonas() {
        return personas;
    }

    public String getPuerta() {
        return puerta;
    }

    public LocalDateTime getMomento() {
        return momento;
    }
}
