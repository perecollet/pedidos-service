package com.perecollet.pedidos.application.service;

import com.perecollet.pedidos.application.exception.PedidoEnConfirmacionException;
import com.perecollet.pedidos.application.exception.PedidoNoEncontradoException;
import com.perecollet.pedidos.application.exception.ProveedorLogisticoException;
import com.perecollet.pedidos.application.port.in.ConfirmarPedidoUseCase;
import com.perecollet.pedidos.application.port.out.PedidoRepositoryPort;
import com.perecollet.pedidos.application.port.out.ProveedorLogisticoPort;
import com.perecollet.pedidos.domain.exception.MonedaNoCoincideException;
import com.perecollet.pedidos.domain.model.Dinero;
import com.perecollet.pedidos.domain.model.EstadoPedido;
import com.perecollet.pedidos.domain.model.Pedido;
import com.perecollet.pedidos.domain.model.PedidoId;
import org.springframework.stereotype.Service;

@Service
public class ConfirmarPedidoService implements ConfirmarPedidoUseCase {

    private final PedidoRepositoryPort pedidoRepositoryPort;
    private final ProveedorLogisticoPort proveedorLogisticoPort;

    public ConfirmarPedidoService(PedidoRepositoryPort pedidoRepositoryPort, ProveedorLogisticoPort proveedorLogisticoPort) {
        this.pedidoRepositoryPort = pedidoRepositoryPort;
        this.proveedorLogisticoPort = proveedorLogisticoPort;
    }

    @Override
    public Pedido confirmar(PedidoId id) {
        if (!pedidoRepositoryPort.reservarParaConfirmacion(id)) {
            return resolverReservaRechazada(id);
        }

        try {
            Pedido pedido = pedidoRepositoryPort.buscarPorId(id)
                    .orElseThrow(() -> new PedidoNoEncontradoException(id));
            Dinero tarifa = proveedorLogisticoPort.obtenerTarifa(
                    pedido.codigoPostalDestino(),
                    pedido.pesoTotalKg(),
                    pedido.numBultos()
            );
            aplicarTarifa(pedido, tarifa);
            pedidoRepositoryPort.guardar(pedido);
            return pedido;
        } catch (RuntimeException e) {
            liberarReserva(id, e);
            throw e;
        }
    }

    private static void aplicarTarifa(Pedido pedido, Dinero tarifa) {
        try {
            pedido.confirmar(tarifa);
        } catch (MonedaNoCoincideException e) {
            throw new ProveedorLogisticoException(
                    "Tarifa del proveedor en moneda no admitida para el pedido %s".formatted(pedido.id().valor()), e);
        }
    }

    // Relee el pedido: si falló el guardado, el de memoria ya está CONFIRMADO y no admite la transición
    private void liberarReserva(PedidoId id, RuntimeException causa) {
        try {
            pedidoRepositoryPort.buscarPorId(id).ifPresent(pedido -> {
                pedido.marcarErrorEnvio();
                pedidoRepositoryPort.guardar(pedido);
            });
        } catch (RuntimeException e) {
            causa.addSuppressed(e);
        }
    }

    private Pedido resolverReservaRechazada(PedidoId id) {
        Pedido pedido = pedidoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new PedidoNoEncontradoException(id));

        if (pedido.estado() != EstadoPedido.CONFIRMADO) {
            throw new PedidoEnConfirmacionException(id);
        }

        return pedido;
    }
}
