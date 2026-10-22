package com.orlandodiaz.ticketing.seguridad.application;

/** PUERTO: cifrado de claves de un solo sentido (hash con sal). Hoy lo implementa BCrypt. */
public interface CifradorDeClaves {

    String cifrar(String claveEnClaro);

    boolean coincide(String claveEnClaro, String claveCifrada);
}
