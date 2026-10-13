package com.orlandodiaz.ticketing.catalogo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DisponibilidadJpaRepository extends JpaRepository<DisponibilidadJpaEntity, DisponibilidadClave> {
}
