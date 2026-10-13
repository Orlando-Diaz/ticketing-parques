package com.orlandodiaz.ticketing.catalogo.infrastructure.persistence;

import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeParques;
import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADAPTADOR de salida: implementa el puerto RepositorioDeParques usando JPA/PostgreSQL.
 * Traduce entre el dominio (Parque) y la tabla (ParqueJpaEntity).
 */
public class RepositorioJpaDeParques implements RepositorioDeParques {

    private final ParqueJpaRepository jpa;

    public RepositorioJpaDeParques(ParqueJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Parque> buscarPorId(ParqueId id) {
        return jpa.findById(id.valor())
                .map(e -> new Parque(new ParqueId(e.getId()), e.getNombre(), e.getCupoDiario()));
    }

    @Override
    @Transactional
    public void guardar(Parque parque) {
        jpa.save(new ParqueJpaEntity(parque.id().valor(), parque.nombre(), parque.cupoDiario()));
    }
}
