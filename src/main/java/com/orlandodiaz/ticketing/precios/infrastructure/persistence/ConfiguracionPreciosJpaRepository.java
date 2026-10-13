package com.orlandodiaz.ticketing.precios.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionPreciosJpaRepository extends JpaRepository<ConfiguracionPreciosJpaEntity, String> {
}
