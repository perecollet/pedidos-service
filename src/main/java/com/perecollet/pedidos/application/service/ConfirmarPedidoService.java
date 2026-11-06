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

        Pedido pedido = pedidoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new PedidoNoEncontradoException(id));

        try {
            Dinero tarifa = proveedorLogisticoPort.obtenerTarifa(
                    pedido.codigoPostalDestino(),
                    pedido.pesoTotalKg(),
                    pedido.numBultos()
            );
            pedido.confirmar(tarifa);
            pedidoRepositoryPort.guardar(pedido);
            return pedido;
        } catch (ProveedorLogisticoException | MonedaNoCoincideException e) {
            pedido.marcarErrorEnvio();
            pedidoRepositoryPort.guardar(pedido);
            throw e;
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
