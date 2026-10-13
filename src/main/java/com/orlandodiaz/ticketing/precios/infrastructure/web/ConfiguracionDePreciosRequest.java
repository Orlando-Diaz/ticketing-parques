package com.orlandodiaz.ticketing.precios.infrastructure.web;

import com.orlandodiaz.ticketing.precios.domain.CategoriaVisitante;
import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.domain.EsquemaDeTarifa;
import com.orlandodiaz.ticketing.precios.domain.ReglaDePrecio;
import com.orlandodiaz.ticketing.precios.domain.TarifaDeCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaPorCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaUnica;
import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoGrupo;
import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoResidente;
import com.orlandodiaz.ticketing.precios.domain.reglas.RecargoFinDeSemana;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.util.List;

/**
 * JSON con el que un parque configura sus precios. Aqui (en el borde) se traduce el JSON
 * a los objetos del dominio; el dominio no sabe nada de JSON ni de textos como "UNICA".
 *
 * esquema.tipo: "UNICA" (usa precio) o "POR_CATEGORIA" (usa categorias; edadMaxima vacia = sin limite).
 * reglas[].tipo: "DESCUENTO_RESIDENTE", "DESCUENTO_GRUPO" o "RECARGO_FIN_DE_SEMANA".
 */
public record ConfiguracionDePreciosRequest(EsquemaRequest esquema, List<ReglaRequest> reglas) {

    public record EsquemaRequest(String tipo, Long precio, List<CategoriaRequest> categorias) {
    }

    public record CategoriaRequest(String nombre, int edadMinima, Integer edadMaxima, long precio) {
    }

    public record ReglaRequest(String tipo, Integer porcentaje, Integer tamanoMinimo) {
    }

    public ConfiguracionDePrecios aDominio(ParqueId parqueId) {
        if (esquema == null) {
            throw new IllegalArgumentException("El esquema de tarifa es obligatorio");
        }
        List<ReglaRequest> reglasPedidas = reglas == null ? List.of() : reglas;
        return new ConfiguracionDePrecios(parqueId, aEsquema(esquema),
                reglasPedidas.stream().map(ConfiguracionDePreciosRequest::aRegla).toList());
    }

    private static EsquemaDeTarifa aEsquema(EsquemaRequest e) {
        return switch (String.valueOf(e.tipo())) {
            case "UNICA" -> new TarifaUnica(Dinero.pesos(requerido(e.precio(), "precio")));
            case "POR_CATEGORIA" -> {
                List<CategoriaRequest> categorias = requerido(e.categorias(), "categorias");
                yield new TarifaPorCategoria(categorias.stream().map(c -> new TarifaDeCategoria(
                        c.edadMaxima() == null
                                ? CategoriaVisitante.desde(c.nombre(), c.edadMinima())
                                : new CategoriaVisitante(c.nombre(), c.edadMinima(), c.edadMaxima()),
                        Dinero.pesos(c.precio()))).toList());
            }
            default -> throw new IllegalArgumentException(
                    "Tipo de esquema desconocido: " + e.tipo() + " (use UNICA o POR_CATEGORIA)");
        };
    }

    private static ReglaDePrecio aRegla(ReglaRequest r) {
        return switch (String.valueOf(r.tipo())) {
            case "DESCUENTO_RESIDENTE" -> new DescuentoResidente(requerido(r.porcentaje(), "porcentaje"));
            case "DESCUENTO_GRUPO" -> new DescuentoGrupo(
                    requerido(r.tamanoMinimo(), "tamanoMinimo"), requerido(r.porcentaje(), "porcentaje"));
            case "RECARGO_FIN_DE_SEMANA" -> new RecargoFinDeSemana(requerido(r.porcentaje(), "porcentaje"));
            default -> throw new IllegalArgumentException("Tipo de regla desconocido: " + r.tipo()
                    + " (use DESCUENTO_RESIDENTE, DESCUENTO_GRUPO o RECARGO_FIN_DE_SEMANA)");
        };
    }

    private static <T> T requerido(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("Falta el campo '" + campo + "'");
        }
        return valor;
    }
}
