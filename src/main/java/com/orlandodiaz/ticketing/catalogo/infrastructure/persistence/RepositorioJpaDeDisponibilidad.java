package com.orlandodiaz.ticketing.catalogo.infrastructure.persistence;

import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.domain.DisponibilidadDiaria;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** ADAPTADOR de salida: guarda y recupera el cupo diario en PostgreSQL. */
public class RepositorioJpaDeDisponibilidad implements RepositorioDeDisponibilidad {

    private final DisponibilidadJpaRepository jpa;

    public RepositorioJpaDeDisponibilidad(DisponibilidadJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DisponibilidadDiaria> buscar(ParqueId parqueId, LocalDate fecha) {
        return jpa.findById(new DisponibilidadClave(parqueId.valor(), fecha))
                .map(e -> DisponibilidadDiaria.reconstruir(
                        new ParqueId(e.getClave().getParqueId()), e.getClave().getFecha(),
                        e.getCupoMaximo(), e.getVendidas()));
    }

    @Override
    @Transactional
    public void guardar(DisponibilidadDiaria d) {
        jpa.save(new DisponibilidadJpaEntity(
                new DisponibilidadClave(d.parqueId().valor(), d.fecha()), d.cupoMaximo(), d.vendidas()));
    }
}
