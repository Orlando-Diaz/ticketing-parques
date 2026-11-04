package com.orlandodiaz.ticketing.seguridad.domain;

/**
 * ADMIN administra todo (parques, precios, reportes, usuarios). PUERTA solo valida ingresos en el acceso.
 * CLIENTE es quien se registra por su cuenta: compra entradas y ve sus propias compras.
 */
public enum Rol {
    ADMIN,
    PUERTA,
    CLIENTE
}
