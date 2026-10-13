package com.orlandodiaz.ticketing.precios.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;

/**
 * Como cobra un parque su entrada. Cada parque elige UN esquema.
 * Quien lo usa (el calculador) solo pide el precio y no le importa cual esquema es:
 * eso es el principio L (sustitucion de Liskov).
 *
 * Es "sealed" (cerrada) a proposito: solo existen estos dos esquemas, y asi el compilador
 * obliga a manejar los dos en cualquier switch. Las REGLAS, en cambio, son abiertas.
 */
public sealed interface EsquemaDeTarifa permits TarifaUnica, TarifaPorCategoria {
    Dinero precioPara(int edadVisitante);
}
