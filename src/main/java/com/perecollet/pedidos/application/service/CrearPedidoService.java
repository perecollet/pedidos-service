package com.perecollet.pedidos.application.service;

import com.perecollet.pedidos.application.exception.ClienteNoEncontradoException;
import com.perecollet.pedidos.application.exception.ProductoNoEncontradoException;
import com.perecollet.pedidos.application.port.in.CrearPedidoCommand;
import com.perecollet.pedidos.application.port.in.CrearPedidoUseCase;
import com.perecollet.pedidos.application.port.out.ClienteRepositoryPort;
import com.perecollet.pedidos.application.port.out.PedidoRepositoryPort;
import com.perecollet.pedidos.application.port.out.ProductoRepositoryPort;
import com.perecollet.pedidos.domain.model.ClienteId;
import com.perecollet.pedidos.domain.model.LineaPedido;
import com.perecollet.pedidos.domain.model.Pedido;
import com.perecollet.pedidos.domain.model.Producto;
import com.perecollet.pedidos.domain.model.ProductoId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CrearPedidoService implements CrearPedidoUseCase {

    private final ClienteRepositoryPort clientesRepository;
    private final ProductoRepositoryPort productosRepository;
    private final PedidoRepositoryPort pedidosRepository;
    private final Clock reloj;

    public CrearPedidoService(ClienteRepositoryPort clientesRepository, ProductoRepositoryPort productosRepository, PedidoRepositoryPort pedidosRepository, Clock reloj) {
        this.clientesRepository = clientesRepository;
        this.productosRepository = productosRepository;
        this.pedidosRepository = pedidosRepository;
        this.reloj = reloj;
    }

    @Transactional
    public Pedido crear(CrearPedidoCommand command) {

        ClienteId clienteId = command.clienteId();

        if (!clientesRepository.existe(clienteId)) {
            throw new ClienteNoEncontradoException(command.clienteId());
        }

        Set<ProductoId> productosIds = command.lineas().stream()
                .map(CrearPedidoCommand.LineaCommand::productoId)
                .collect(Collectors.toSet());

        Map<ProductoId, Producto> catalogo = productosRepository.buscarPorIds(productosIds);

        List<LineaPedido> lineas = command.lineas().stream()
                .map(linea -> LineaPedido.of(buscarProducto(catalogo, linea.productoId()), linea.cantidad()))
                .toList();

        Pedido pedido = Pedido.crear(clienteId, command.codigoPostalDestino(), lineas, Instant.now(reloj));
        pedidosRepository.guardar(pedido);

        return pedido;
    }

    private static Producto buscarProducto(Map<ProductoId, Producto> catalogo, ProductoId id) {
        return Optional.ofNullable(catalogo.get(id))
                .orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

}
