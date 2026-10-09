package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Orquesta las reglas. Su unica responsabilidad es combinar los ajustes (principio S).
 * No sabe cuales reglas existen: se las pasan por el constructor (principio D).
 *
 * Politica actual: los descuentos NO se acumulan (se aplica solo el mayor);
 * los recargos si se suman. Todo se calcula sobre el precio base.
 */
public class CalculadorDePrecio {

    private final List<ReglaDePrecio> reglas;

    public CalculadorDePrecio(List<ReglaDePrecio> reglas) {
        this.reglas = List.copyOf(reglas);
    }

    public DesglosePrecio calcular(ContextoDePrecio contexto) {
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

        Dinero total = contexto.precioBase();
        for (Ajuste ajuste : aplicados) {
            total = ajuste.tipo() == TipoAjuste.DESCUENTO
                    ? total.restar(ajuste.monto())
                    : total.sumar(ajuste.monto());
        }
        return new DesglosePrecio(contexto.precioBase(), List.copyOf(aplicados), total);
    }
}
