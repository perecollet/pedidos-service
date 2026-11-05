package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.application.port.out.ProductoRepositoryPort;
import com.perecollet.pedidos.domain.model.Producto;
import com.perecollet.pedidos.domain.model.ProductoId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class ProductoPersistenceAdapter implements ProductoRepositoryPort {

    private final ProductoJpaRepository productoJpaRepository;

    public ProductoPersistenceAdapter(ProductoJpaRepository productoJpaRepository) {
        this.productoJpaRepository = productoJpaRepository;
    }

    @Override
    public Map<ProductoId, Producto> buscarPorIds(Set<ProductoId> ids) {
        List<UUID> uuids = ids.stream()
                .map(ProductoId::valor)
                .toList();

        return productoJpaRepository.findAllById(uuids).stream()
                .map(ProductoMapper::toDomain)
                .collect(Collectors.toMap(Producto::id, Function.identity()));
    }
}
