package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;

/**
 * Como cobra un parque su entrada. Cada parque elige UN esquema.
 * Quien lo usa (el calculador) solo pide el precio y no le importa cual esquema es:
 * eso es el principio L (sustitucion de Liskov).
 */
public interface EsquemaDeTarifa {
    Dinero precioPara(int edadVisitante);
}
