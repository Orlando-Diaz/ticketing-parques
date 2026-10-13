package com.orlandodiaz.ticketing.precios.domain;

/**
 * Forma "plana" de una regla de precio: su tipo y sus parametros. Sirve para guardar una regla
 * (base de datos) o recibirla (JSON) sin que esas capas tengan que conocer cada clase concreta.
 */
public record DescripcionDeRegla(String tipo, Integer porcentaje, Integer tamanoMinimo) {
}
