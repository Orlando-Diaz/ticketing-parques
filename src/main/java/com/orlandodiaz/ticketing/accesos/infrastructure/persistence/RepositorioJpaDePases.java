package com.orlandodiaz.ticketing.accesos.infrastructure.persistence;

import com.orlandodiaz.ticketing.accesos.application.RepositorioDePases;
import com.orlandodiaz.ticketing.accesos.domain.EstadoPase;
import com.orlandodiaz.ticketing.accesos.domain.Ingreso;
import com.orlandodiaz.ticketing.accesos.domain.Pase;
import com.orlandodiaz.ticketing.shared.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/** ADAPTADOR de salida: guarda los pases y su historial de ingresos en PostgreSQL. */
public class RepositorioJpaDePases implements RepositorioDePases {

    private final PaseJpaRepository jpa;

    public RepositorioJpaDePases(PaseJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pase> buscarPorCodigo(String codigo) {
        return jpa.findByCodigo(codigo).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pase> buscarPorOrden(String ordenRef) {
        return jpa.findByOrdenRef(ordenRef).map(this::aDominio);
    }

    /**
     * - Sin version (pase nuevo): INSERT. Si ya existe ese codigo u orden, los UNIQUE de la tabla lo rechazan.
     * - Con version: solo se actualiza si la fila sigue en esa version; se agregan SOLO los ingresos nuevos
     *   (el historial solo crece). Si otra puerta se adelanto, se lanza ConflictoDeConcurrenciaException.
     */
    @Override
    @Transactional
    public void guardar(Pase pase) {
        try {
            if (pase.version() == null) {
                jpa.saveAndFlush(nueva(pase));
                return;
            }
            PaseJpaEntity entidad = jpa.findById(pase.id()).orElseThrow(
                    () -> new ConflictoDeConcurrenciaException("El pase " + pase.id() + " ya no existe"));
            if (!Objects.equals(entidad.getVersion(), pase.version())) {
                throw new ConflictoDeConcurrenciaException("El pase " + pase.id() + " fue modificado por otra operacion");
            }
            entidad.actualizar(pase.estado().name(), pase.entradasUsadas());
            List<Ingreso> todos = pase.ingresos();
            for (int i = entidad.getIngresos().size(); i < todos.size(); i++) {
                entidad.agregarIngreso(aEntidad(i, todos.get(i)));
            }
            jpa.saveAndFlush(entidad);
        } catch (ConcurrencyFailureException | DataIntegrityViolationException e) {
            throw new ConflictoDeConcurrenciaException("El pase " + pase.id() + " cambio mientras se modificaba", e);
        }
    }

    private PaseJpaEntity nueva(Pase p) {
        PaseJpaEntity entidad = new PaseJpaEntity(p.id(), p.codigo(), p.ordenRef(), p.parqueId().valor(),
                p.fechaVisita(), p.entradasTotales(), p.entradasUsadas(), p.estado().name(), p.emitidoEn());
        List<Ingreso> ingresos = p.ingresos();
        for (int i = 0; i < ingresos.size(); i++) {
            entidad.agregarIngreso(aEntidad(i, ingresos.get(i)));
        }
        return entidad;
    }

    private static IngresoJpaEntity aEntidad(int posicion, Ingreso i) {
        return new IngresoJpaEntity(posicion, i.personas(), i.puerta(), i.momento());
    }

    private Pase aDominio(PaseJpaEntity e) {
        List<Ingreso> ingresos = e.getIngresos().stream()
                .map(i -> new Ingreso(i.getPersonas(), i.getPuerta(), i.getMomento()))
                .toList();
        return Pase.reconstruir(e.getId(), e.getCodigo(), e.getOrdenRef(), new ParqueId(e.getParqueId()),
                e.getFechaVisita(), e.getEntradasTotales(), e.getEmitidoEn(), EstadoPase.valueOf(e.getEstado()),
                ingresos, e.getVersion());
    }
}
