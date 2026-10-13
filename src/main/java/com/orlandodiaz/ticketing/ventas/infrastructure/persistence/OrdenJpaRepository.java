package com.orlandodiaz.ticketing.ventas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenJpaRepository extends JpaRepository<OrdenJpaEntity, String> {
}
