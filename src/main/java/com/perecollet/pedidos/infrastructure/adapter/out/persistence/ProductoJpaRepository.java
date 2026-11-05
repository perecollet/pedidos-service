package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductoJpaRepository extends JpaRepository<ProductoEntity, UUID> {
}
