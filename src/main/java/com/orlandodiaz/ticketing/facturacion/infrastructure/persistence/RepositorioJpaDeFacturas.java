package com.orlandodiaz.ticketing.facturacion.infrastructure.persistence;

import com.orlandodiaz.ticketing.facturacion.application.RepositorioDeFacturas;
import com.orlandodiaz.ticketing.facturacion.domain.EstadoFactura;
import com.orlandodiaz.ticketing.facturacion.domain.Factura;
import com.orlandodiaz.ticketing.facturacion.domain.LineaDeFactura;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** ADAPTADOR de salida: guarda las facturas en PostgreSQL. */
public class RepositorioJpaDeFacturas implements RepositorioDeFacturas {

    private final FacturaJpaRepository jpa;

    public RepositorioJpaDeFacturas(FacturaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void guardar(Factura factura) {
        // Una factura solo cambia de estado despues de emitida; si ya existe, solo actualizamos el estado.
        FacturaJpaEntity entidad = jpa.findById(factura.id()).orElseGet(() -> nueva(factura));
        entidad.cambiarEstado(factura.estado().name());
        jpa.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Factura> buscarPorOrden(String ordenRef) {
        return jpa.findByOrdenRef(ordenRef).map(this::aDominio);
    }

    private FacturaJpaEntity nueva(Factura f) {
        FacturaJpaEntity entidad = new FacturaJpaEntity(f.id(), f.numero(), f.ordenRef(), f.parqueId().valor(),
                f.total().pesos(), f.emitidaEn(), f.estado().name());
        int posicion = 0;
        for (LineaDeFactura l : f.lineas()) {
            entidad.agregarLinea(new LineaFacturaJpaEntity(posicion++, l.descripcion(), l.valor().pesos()));
        }
        return entidad;
    }

    private Factura aDominio(FacturaJpaEntity e) {
        List<LineaDeFactura> lineas = e.getLineas().stream()
                .map(l -> new LineaDeFactura(l.getDescripcion(), Dinero.pesos(l.getValor())))
                .toList();
        return Factura.reconstruir(e.getId(), e.getNumero(), e.getOrdenRef(), new ParqueId(e.getParqueId()),
                lineas, e.getEmitidaEn(), EstadoFactura.valueOf(e.getEstado()));
    }
}
