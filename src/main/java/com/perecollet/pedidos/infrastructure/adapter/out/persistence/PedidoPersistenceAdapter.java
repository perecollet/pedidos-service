package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.application.port.out.PedidoRepositoryPort;
import com.perecollet.pedidos.domain.model.EstadoPedido;
import com.perecollet.pedidos.domain.model.Pedido;
import com.perecollet.pedidos.domain.model.PedidoId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class PedidoPersistenceAdapter implements PedidoRepositoryPort {

    private final PedidoJpaRepository pedidoJpaRepository;

    public PedidoPersistenceAdapter(PedidoJpaRepository pedidoJpaRepository) {
        this.pedidoJpaRepository = pedidoJpaRepository;
    }

    @Override
    public void guardar(Pedido pedido) {
        pedidoJpaRepository.save(PedidoMapper.toEntity(pedido));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPorId(PedidoId id) {
        return pedidoJpaRepository.findById(id.valor())
                .map(PedidoMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean reservarParaConfirmacion(PedidoId id){
        int filas = pedidoJpaRepository.cambiarEstadoSi(
                id.valor(), EstadoPedido.CONFIRMANDO, EstadoPedido.confirmables());
        return filas == 1;
    }
}
