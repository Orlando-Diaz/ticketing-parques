package com.orlandodiaz.ticketing.accesos.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaseJpaRepository extends JpaRepository<PaseJpaEntity, String> {

    Optional<PaseJpaEntity> findByCodigo(String codigo);

    Optional<PaseJpaEntity> findByOrdenRef(String ordenRef);
}
