package com.perecollet.pedidos.application.port.out;

import com.perecollet.pedidos.domain.model.Pedido;

public interface PedidoRepositoryPort {

    void guardar(Pedido pedido);
}
