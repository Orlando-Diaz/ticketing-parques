package com.orlandodiaz.ticketing.ventas.infrastructure.persistence;

import com.orlandodiaz.ticketing.shared.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.ventas.application.RepositorioDeOrdenes;
import com.orlandodiaz.ticketing.ventas.domain.EstadoOrden;
import com.orlandodiaz.ticketing.ventas.domain.LineaDeOrden;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/** ADAPTADOR de salida: guarda las ordenes en PostgreSQL. */
public class RepositorioJpaDeOrdenes implements RepositorioDeOrdenes {

    private final OrdenJpaRepository jpa;

    public RepositorioJpaDeOrdenes(OrdenJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * Una orden solo cambia de estado despues de creada (sus lineas no cambian).
     * - Sin version (orden nueva): INSERT.
     * - Con version: solo se actualiza si la fila sigue en esa version; si otro ya la modifico (por ejemplo,
     *   otra cancelacion simultanea) se lanza ConflictoDeConcurrenciaException. La comparacion explicita cubre
     *   "alguien guardo antes de que yo leyera de nuevo"; el @Version de la entidad cubre la carrera entre
     *   esta lectura y el UPDATE.
     */
    @Override
    @Transactional
    public void guardar(Orden orden) {
        try {
            if (orden.version() == null) {
                jpa.saveAndFlush(nueva(orden));
                return;
            }
            OrdenJpaEntity entidad = jpa.findById(orden.id().valor()).orElseThrow(
                    () -> new ConflictoDeConcurrenciaException("La orden " + orden.id().valor() + " ya no existe"));
            if (!Objects.equals(entidad.getVersion(), orden.version())) {
                throw new ConflictoDeConcurrenciaException(
                        "La orden " + orden.id().valor() + " fue modificada por otra operacion");
            }
            entidad.cambiarEstado(orden.estado().name());
            jpa.saveAndFlush(entidad);
        } catch (ConcurrencyFailureException | DataIntegrityViolationException e) {
            throw new ConflictoDeConcurrenciaException(
                    "La orden " + orden.id().valor() + " cambio mientras se modificaba", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Orden> buscarPorId(OrdenId id) {
        return jpa.findById(id.valor()).map(this::aDominio);
    }

    private OrdenJpaEntity nueva(Orden orden) {
        OrdenJpaEntity entidad = new OrdenJpaEntity(orden.id().valor(), orden.parqueId().valor(),
                orden.fechaVisita(), orden.creadaEn(), orden.total().pesos(), orden.estado().name());
        int posicion = 0;
        for (LineaDeOrden l : orden.lineas()) {
            entidad.agregarLinea(new LineaOrdenJpaEntity(
                    posicion++, l.edadVisitante(), l.residente(), l.precio().pesos()));
        }
        return entidad;
    }

    private Orden aDominio(OrdenJpaEntity e) {
        List<LineaDeOrden> lineas = e.getLineas().stream()
                .map(l -> new LineaDeOrden(l.getEdadVisitante(), l.isResidente(), Dinero.pesos(l.getPrecio())))
                .toList();
        return Orden.reconstruir(new OrdenId(e.getId()), new ParqueId(e.getParqueId()), e.getFechaVisita(),
                lineas, e.getCreadaEn(), EstadoOrden.valueOf(e.getEstado()), e.getVersion());
    }
}
