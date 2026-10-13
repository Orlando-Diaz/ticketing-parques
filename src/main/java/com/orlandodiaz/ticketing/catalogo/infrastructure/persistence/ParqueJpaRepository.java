package com.orlandodiaz.ticketing.catalogo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data genera la implementacion (guardar, buscar...) a partir de esta interfaz. */
public interface ParqueJpaRepository extends JpaRepository<ParqueJpaEntity, String> {
}
