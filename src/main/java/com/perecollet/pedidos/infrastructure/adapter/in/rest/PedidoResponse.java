package com.perecollet.pedidos.infrastructure.adapter.in.rest;

import com.perecollet.pedidos.domain.model.Dinero;
import com.perecollet.pedidos.domain.model.EstadoPedido;
import com.perecollet.pedidos.domain.model.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PedidoResponse(UUID id, UUID clienteId, String codigoPostalDestino, Instant fecha,
                             EstadoPedido estado, BigDecimal importeProductos,
                             BigDecimal costeEnvio, BigDecimal importeTotal) {

    static PedidoResponse from(Pedido p) {
        return new PedidoResponse(p.id().valor(), p.clienteId().valor(), p.codigoPostalDestino(), p.fecha(),
                p.estado(), p.importeProductos().importe(),
                p.costeEnvio().map(Dinero::importe).orElse(null),
                p.importeTotal().map(Dinero::importe).orElse(null));
    }
}