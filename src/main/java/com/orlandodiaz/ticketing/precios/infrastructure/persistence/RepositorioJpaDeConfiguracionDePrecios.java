package com.orlandodiaz.ticketing.precios.infrastructure.persistence;

import com.orlandodiaz.ticketing.precios.application.RepositorioDeConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.domain.CategoriaVisitante;
import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.domain.DescripcionDeRegla;
import com.orlandodiaz.ticketing.precios.domain.EsquemaDeTarifa;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TarifaDeCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaPorCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaUnica;
import com.orlandodiaz.ticketing.precios.domain.reglas.FabricaDeReglas;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADAPTADOR de salida: guarda la configuracion de precios en tablas.
 * Las reglas se guardan por su descripcion (tipo + parametros) y se recrean con FabricaDeReglas,
 * asi este adaptador NO necesita conocer cada clase de regla.
 */
public class RepositorioJpaDeConfiguracionDePrecios implements RepositorioDeConfiguracionDePrecios {

    private final ConfiguracionPreciosJpaRepository jpa;

    public RepositorioJpaDeConfiguracionDePrecios(ConfiguracionPreciosJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionDePrecios> buscarPorParque(ParqueId parqueId) {
        return jpa.findById(parqueId.valor()).map(this::aDominio);
    }

    @Override
    @Transactional
    public void guardar(ConfiguracionDePrecios configuracion) {
        String id = configuracion.parqueId().valor();
        ConfiguracionPreciosJpaEntity entidad = jpa.findById(id).orElseGet(() -> new ConfiguracionPreciosJpaEntity(id));
        entidad.limpiar();

        // EsquemaDeTarifa es "sealed": el compilador garantiza que aqui estan todos los casos.
        switch (configuracion.esquema()) {
            case TarifaUnica unica -> entidad.definirUnica(unica.precio().pesos());
            case TarifaPorCategoria porCategoria -> {
                entidad.definirPorCategoria();
                int posicion = 0;
                for (TarifaDeCategoria t : porCategoria.tarifas()) {
                    CategoriaVisitante c = t.categoria();
                    Integer edadMaxima = c.edadMaxima() == Integer.MAX_VALUE ? null : c.edadMaxima();
                    entidad.agregarCategoria(new CategoriaTarifaJpaEntity(
                            posicion++, c.nombre(), c.edadMinima(), edadMaxima, t.precio().pesos()));
                }
            }
        }

        int posicionRegla = 0;
        for (ReglaDePrecio regla : configuracion.reglas()) {
            DescripcionDeRegla d = regla.describir();
            entidad.agregarRegla(new ReglaPrecioJpaEntity(posicionRegla++, d.tipo(), d.porcentaje(), d.tamanoMinimo()));
        }
        jpa.save(entidad);
    }

    private ConfiguracionDePrecios aDominio(ConfiguracionPreciosJpaEntity e) {
        EsquemaDeTarifa esquema = switch (e.getEsquemaTipo()) {
            case "UNICA" -> new TarifaUnica(Dinero.pesos(e.getPrecioUnico()));
            case "POR_CATEGORIA" -> new TarifaPorCategoria(e.getCategorias().stream()
                    .map(c -> new TarifaDeCategoria(
                            c.getEdadMaxima() == null
                                    ? CategoriaVisitante.desde(c.getNombre(), c.getEdadMinima())
                                    : new CategoriaVisitante(c.getNombre(), c.getEdadMinima(), c.getEdadMaxima()),
                            Dinero.pesos(c.getPrecio())))
                    .toList());
            default -> throw new IllegalStateException("Esquema de tarifa guardado desconocido: " + e.getEsquemaTipo());
        };
        List<ReglaDePrecio> reglas = e.getReglas().stream()
                .map(r -> FabricaDeReglas.crear(new DescripcionDeRegla(r.getTipo(), r.getPorcentaje(), r.getTamanoMinimo())))
                .toList();
        return new ConfiguracionDePrecios(new ParqueId(e.getParqueId()), esquema, reglas);
    }
}
