package com.perecollet.pedidos.application.port.out;

import com.perecollet.pedidos.domain.model.Pedido;
import com.perecollet.pedidos.domain.model.PedidoId;

import java.util.Optional;

public interface PedidoRepositoryPort {

    void guardar(Pedido pedido);

    Optional<Pedido> buscarPorId(PedidoId id);
}
