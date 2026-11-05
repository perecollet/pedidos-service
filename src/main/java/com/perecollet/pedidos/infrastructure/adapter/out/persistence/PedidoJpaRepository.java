package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.domain.model.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, UUID> {

    @Modifying
    @Query("""
            UPDATE PedidoEntity p
            SET p.estado = :nuevo
            WHERE p.id = :id AND p.estado IN :desde
            """)
    int cambiarEstadoSi(@Param("id") UUID id,
                        @Param("nuevo") EstadoPedido nuevo,
                        @Param("desde") Collection<EstadoPedido> desde);
}
