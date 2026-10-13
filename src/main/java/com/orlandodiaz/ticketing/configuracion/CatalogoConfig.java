package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.catalogo.application.ConsultarCupoDisponible;
import com.orlandodiaz.ticketing.catalogo.application.LiberarCupo;
import com.orlandodiaz.ticketing.catalogo.application.RegistrarParque;
import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeParques;
import com.orlandodiaz.ticketing.catalogo.application.ReservarCupo;
import com.orlandodiaz.ticketing.catalogo.infrastructure.persistence.DisponibilidadJpaRepository;
import com.orlandodiaz.ticketing.catalogo.infrastructure.persistence.ParqueJpaRepository;
import com.orlandodiaz.ticketing.catalogo.infrastructure.persistence.RepositorioJpaDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.infrastructure.persistence.RepositorioJpaDeParques;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogoConfig {

    // --- adaptadores de salida: PostgreSQL via JPA (cambiarlos por otros no toca ningun caso de uso) ---
    @Bean
    RepositorioDeParques repositorioDeParques(ParqueJpaRepository jpa) {
        return new RepositorioJpaDeParques(jpa);
    }

    @Bean
    RepositorioDeDisponibilidad repositorioDeDisponibilidad(DisponibilidadJpaRepository jpa) {
        return new RepositorioJpaDeDisponibilidad(jpa);
    }

    // --- casos de uso ---
    @Bean
    RegistrarParque registrarParque(RepositorioDeParques parques) {
        return new RegistrarParque(parques);
    }

    @Bean
    ReservarCupo reservarCupo(RepositorioDeParques parques, RepositorioDeDisponibilidad disponibilidades) {
        return new ReservarCupo(parques, disponibilidades);
    }

    @Bean
    LiberarCupo liberarCupo(RepositorioDeDisponibilidad disponibilidades) {
        return new LiberarCupo(disponibilidades);
    }

    @Bean
    ConsultarCupoDisponible consultarCupoDisponible(RepositorioDeParques parques,
                                                    RepositorioDeDisponibilidad disponibilidades) {
        return new ConsultarCupoDisponible(parques, disponibilidades);
    }
}
