package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.domain.model.ClienteId;
import com.perecollet.pedidos.domain.model.Dinero;
import com.perecollet.pedidos.domain.model.LineaPedido;
import com.perecollet.pedidos.domain.model.Pedido;
import com.perecollet.pedidos.domain.model.PedidoId;
import com.perecollet.pedidos.domain.model.ProductoId;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

public final class PedidoMapper {

    private PedidoMapper() {
    }

    static PedidoEntity toEntity(Pedido pedido) {
        return new PedidoEntity(
                pedido.id().valor(),
                pedido.clienteId().valor(),
                pedido.codigoPostalDestino(),
                pedido.fecha(),
                pedido.estado(),
                pedido.importeProductos().moneda().getCurrencyCode(),
                pedido.importeProductos().importe(),
                pedido.costeEnvio().map(Dinero::importe).orElse(null),
                pedido.importeTotal().map(Dinero::importe).orElse(null),
                pedido.lineas().stream()
                        .map(PedidoMapper::toEmbeddable)
                        .collect(Collectors.toCollection(ArrayList::new))
        );
    }

    private static LineaPedidoEmbeddable toEmbeddable(LineaPedido linea) {
        return new LineaPedidoEmbeddable(
                linea.productoId().valor(),
                linea.cantidad(),
                linea.precioUnitario().importe(),
                linea.precioUnitario().moneda().getCurrencyCode(),
                linea.pesoKg()
        );
    }

    static Pedido toDomain(PedidoEntity entidad) {
        List<LineaPedido> lineas = entidad.getLineas().stream()
                .map(PedidoMapper::toDomain)
                .toList();

        Dinero costeEnvio = entidad.getCosteEnvio() == null
                ? null
                : Dinero.of(entidad.getCosteEnvio(), Currency.getInstance(entidad.getMoneda()));

        return Pedido.reconstituir(
                new PedidoId(entidad.getId()),
                new ClienteId(entidad.getClienteId()),
                entidad.getCodigoPostalDestino(),
                lineas,
                entidad.getFecha(),
                entidad.getEstado(),
                costeEnvio
        );
    }

    private static LineaPedido toDomain(LineaPedidoEmbeddable linea) {
        return new LineaPedido(
                new ProductoId(linea.getProductoId()),
                linea.getCantidad(),
                Dinero.of(linea.getPrecioUnitario(), Currency.getInstance(linea.getMoneda())),
                linea.getPesoKg()
        );
    }
}
