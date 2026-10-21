package com.orlandodiaz.ticketing.catalogo.infrastructure.persistence;

import com.orlandodiaz.ticketing.catalogo.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.domain.DisponibilidadDiaria;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
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
                        e.getCupoMaximo(), e.getVendidas(), e.getVersion()));
    }

    /**
     * Si la disponibilidad trae version, JPA hace "UPDATE ... WHERE version = <la que se leyo>": si otro ya
     * guardo, falla (ConcurrencyFailureException). Si es nueva (sin version), hace INSERT: si otro la creo
     * primero, la llave primaria lo rechaza (DataIntegrityViolationException). En ambos casos el puerto
     * espera la misma excepcion: ConflictoDeConcurrenciaException.
     *
     * saveAndFlush (y no save) fuerza el SQL aqui mismo, dentro del try, y no al cerrar la transaccion.
     */
    @Override
    @Transactional
    public void guardar(DisponibilidadDiaria d) {
        var entidad = new DisponibilidadJpaEntity(
                new DisponibilidadClave(d.parqueId().valor(), d.fecha()), d.cupoMaximo(), d.vendidas(), d.version());
        try {
            jpa.saveAndFlush(entidad);
        } catch (ConcurrencyFailureException | DataIntegrityViolationException e) {
            throw new ConflictoDeConcurrenciaException(
                    "El cupo de " + d.parqueId().valor() + " el " + d.fecha() + " cambio mientras se modificaba", e);
        }
    }
}
