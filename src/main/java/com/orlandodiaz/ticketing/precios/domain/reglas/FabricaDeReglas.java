package com.orlandodiaz.ticketing.precios.domain.reglas;

import com.orlandodiaz.ticketing.precios.domain.DescripcionDeRegla;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;

/**
 * Crea una regla a partir de su descripcion. Es el UNICO lugar que conoce todos los tipos:
 * para agregar una regla nueva se crea su clase y se anade una linea aqui. Ni la API ni la
 * base de datos necesitan cambiar.
 */
public final class FabricaDeReglas {

    private FabricaDeReglas() {
    }

    public static ReglaDePrecio crear(DescripcionDeRegla d) {
        return switch (String.valueOf(d.tipo())) {
            case DescuentoResidente.TIPO -> new DescuentoResidente(requerido(d.porcentaje(), "porcentaje"));
            case DescuentoGrupo.TIPO -> new DescuentoGrupo(
                    requerido(d.tamanoMinimo(), "tamanoMinimo"), requerido(d.porcentaje(), "porcentaje"));
            case RecargoFinDeSemana.TIPO -> new RecargoFinDeSemana(requerido(d.porcentaje(), "porcentaje"));
            default -> throw new IllegalArgumentException("Tipo de regla desconocido: " + d.tipo()
                    + " (use " + DescuentoResidente.TIPO + ", " + DescuentoGrupo.TIPO
                    + " o " + RecargoFinDeSemana.TIPO + ")");
        };
    }

    private static int requerido(Integer valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("Falta el campo '" + campo + "'");
        }
        return valor;
    }
}
