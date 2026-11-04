package com.orlandodiaz.ticketing.ventas.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenJpaRepository extends JpaRepository<OrdenJpaEntity, String> {

    List<OrdenJpaEntity> findByClienteOrderByCreadaEnDesc(String cliente);
}
