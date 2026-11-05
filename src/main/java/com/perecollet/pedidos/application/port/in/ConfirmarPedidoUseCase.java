package com.perecollet.pedidos.application.port.in;

import com.perecollet.pedidos.domain.model.Pedido;
import com.perecollet.pedidos.domain.model.PedidoId;

public interface ConfirmarPedidoUseCase {

    Pedido confirmar(PedidoId pedidoId);
}
