package com.orlandodiaz.ticketing.seguridad.infrastructure;

import com.orlandodiaz.ticketing.seguridad.application.CifradorDeClaves;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Adaptador: BCrypt (lento a proposito y con sal propia por clave, asi las fuerza bruta cuesta mucho). */
public class CifradorBCrypt implements CifradorDeClaves {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String cifrar(String claveEnClaro) {
        return bcrypt.encode(claveEnClaro);
    }

    @Override
    public boolean coincide(String claveEnClaro, String claveCifrada) {
        return bcrypt.matches(claveEnClaro, claveCifrada);
    }
}
