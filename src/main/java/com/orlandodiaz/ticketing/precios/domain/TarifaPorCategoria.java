package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import java.util.List;

/** Un precio distinto por categoria de visitante (ej. nino, adulto, adulto mayor). */
public record TarifaPorCategoria(List<TarifaDeCategoria> tarifas) implements EsquemaDeTarifa {

    public TarifaPorCategoria {
        if (tarifas == null || tarifas.isEmpty()) {
            throw new IllegalArgumentException("Debe haber al menos una categoria con tarifa");
        }
        tarifas = List.copyOf(tarifas);
        for (int i = 0; i < tarifas.size(); i++) {
            for (int j = i + 1; j < tarifas.size(); j++) {
                CategoriaVisitante a = tarifas.get(i).categoria();
                CategoriaVisitante b = tarifas.get(j).categoria();
                if (a.seSolapaCon(b)) {
                    throw new IllegalArgumentException(
                            "Las categorias '" + a.nombre() + "' y '" + b.nombre() + "' se solapan");
                }
            }
        }
    }

    @Override
    public Dinero precioPara(int edadVisitante) {
        return tarifas.stream()
                .filter(t -> t.categoria().incluye(edadVisitante))
                .map(TarifaDeCategoria::precio)
                .findFirst()
                .orElseThrow(() -> new SinTarifaParaEdadException(edadVisitante));
    }
}
