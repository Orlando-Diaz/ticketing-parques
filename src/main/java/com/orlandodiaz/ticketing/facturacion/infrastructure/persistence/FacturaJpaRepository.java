package com.orlandodiaz.ticketing.facturacion.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaJpaRepository extends JpaRepository<FacturaJpaEntity, String> {

    // Spring Data crea la consulta a partir del nombre del metodo: ... WHERE orden_ref = ?
    Optional<FacturaJpaEntity> findByOrdenRef(String ordenRef);
}
