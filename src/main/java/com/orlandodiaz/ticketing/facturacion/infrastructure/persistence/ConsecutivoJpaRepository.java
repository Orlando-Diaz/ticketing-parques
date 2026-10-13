package com.orlandodiaz.ticketing.facturacion.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsecutivoJpaRepository extends JpaRepository<ConsecutivoJpaEntity, String> {

    @Modifying
    @Query("update ConsecutivoJpaEntity c set c.ultimo = c.ultimo + 1 where c.id = :id")
    int incrementar(@Param("id") String id);

    @Query("select c.ultimo from ConsecutivoJpaEntity c where c.id = :id")
    long ultimo(@Param("id") String id);
}
