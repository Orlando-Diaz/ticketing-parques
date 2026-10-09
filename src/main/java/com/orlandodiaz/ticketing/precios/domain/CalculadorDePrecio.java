package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Calcula el precio de una entrada para UN parque.
 * Paso 1: el esquema de tarifa del parque da el precio base.
 * Paso 2: las reglas activas del parque ajustan ese precio (descuentos y recargos).
 *
 * No sabe que esquema ni que reglas existen: se las entregan por el constructor (principio D).
 * Cada parque tiene su propio calculador con su propia configuracion.
 *
 * Politica actual: los descuentos NO se acumulan (se aplica solo el mayor);
 * los recargos si se suman. Todo se calcula sobre el precio base.
 */
public class CalculadorDePrecio {

    private final EsquemaDeTarifa esquema;
    private final List<ReglaDePrecio> reglas;

    public CalculadorDePrecio(EsquemaDeTarifa esquema, List<ReglaDePrecio> reglas) {
        this.esquema = Objects.requireNonNull(esquema, "El esquema de tarifa es obligatorio");
        this.reglas = List.copyOf(reglas);
    }

    public DesglosePrecio calcular(DatosDeVisita visita) {
        Dinero precioBase = esquema.precioPara(visita.edadVisitante());
        ContextoDePrecio contexto = new ContextoDePrecio(
                precioBase, visita.edadVisitante(), visita.residente(),
                visita.tamanoGrupo(), visita.fechaVisita());

        List<Ajuste> candidatos = reglas.stream()
                .map(regla -> regla.evaluar(contexto))
                .flatMap(Optional::stream)
                .toList();

        List<Ajuste> aplicados = new ArrayList<>();
        candidatos.stream()
                .filter(a -> a.tipo() == TipoAjuste.DESCUENTO)
                .max(Comparator.comparing(Ajuste::monto))
                .ifPresent(aplicados::add);
        candidatos.stream()
                .filter(a -> a.tipo() == TipoAjuste.RECARGO)
                .forEach(aplicados::add);

        Dinero total = precioBase;
        for (Ajuste ajuste : aplicados) {
            total = ajuste.tipo() == TipoAjuste.DESCUENTO
                    ? total.restar(ajuste.monto())
                    : total.sumar(ajuste.monto());
        }
        return new DesglosePrecio(precioBase, List.copyOf(aplicados), total);
    }
}
