package com.perecollet.pedidos.domain.model;

import com.perecollet.pedidos.domain.exception.MonedaNoCoincideException;
import com.perecollet.pedidos.domain.exception.TransicionEstadoInvalidaException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Pedido {

    private static final Currency MONEDA = Currency.getInstance("EUR");

    private final PedidoId id;
    private final ClienteId clienteId;
    private final String codigoPostalDestino;
    private final List<LineaPedido> lineas;
    private final Instant fecha;

    private EstadoPedido estado;
    private Dinero costeEnvio;

    private Pedido(PedidoId id, ClienteId clienteId, String codigoPostalDestino, List<LineaPedido> lineas, Instant fecha, EstadoPedido estado, Dinero costeEnvio) {
        this.id = Objects.requireNonNull(id, "pedidoId");
        this.clienteId = Objects.requireNonNull(clienteId, "clienteId");
        this.codigoPostalDestino = Objects.requireNonNull(codigoPostalDestino, "codigoPostalDestino");

        this.lineas = List.copyOf(Objects.requireNonNull(lineas, "lineas"));
        if (this.lineas.isEmpty()) {
            throw new IllegalArgumentException("Un pedido debe tener al menos una línea");
        }
        this.lineas.stream()
                .map(l -> l.precioUnitario().moneda())
                .filter(moneda -> !moneda.equals(MONEDA))
                .findFirst()
                .ifPresent(moneda -> {
                    throw new MonedaNoCoincideException(MONEDA, moneda);
                });

        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.estado = Objects.requireNonNull(estado, "estado");

        boolean confirmado = estado == EstadoPedido.CONFIRMADO;
        boolean tieneCoste = costeEnvio != null;
        if (confirmado != tieneCoste) {
            throw new IllegalArgumentException(
                    "Un pedido %s %s tener coste de envío".formatted(estado, confirmado ? "debe" : "no puede"));
        }

        this.costeEnvio = costeEnvio;
    }

    public static Pedido crear(ClienteId clienteId, String codigoPostalDestino, List<LineaPedido> lineas, Instant fecha) {
        return new Pedido(PedidoId.nuevo(), clienteId, codigoPostalDestino, lineas, fecha, EstadoPedido.CREADO, null);
    }

    public static Pedido reconstituir(PedidoId id, ClienteId clienteId, String codigoPostalDestino, List<LineaPedido> lineas, Instant fecha, EstadoPedido estado, Dinero costeEnvio) {
        return new Pedido(id, clienteId, codigoPostalDestino, lineas, fecha, estado, costeEnvio);
    }

    public Dinero importeProductos() {
        return lineas.stream()
                .map(LineaPedido::importe)
                .reduce(Dinero.cero(MONEDA), Dinero::sumar);
    }

    public Optional<Dinero> costeEnvio() {
        return Optional.ofNullable(costeEnvio);
    }

    public Optional<Dinero> importeTotal() {
        return costeEnvio().map(importeProductos()::sumar);
    }

    public void confirmar(Dinero coste) {
        Objects.requireNonNull(coste, "coste");
        exigirEstadoConfirmando(EstadoPedido.CONFIRMADO);
        if (!coste.moneda().equals(MONEDA)) {
            throw new MonedaNoCoincideException(MONEDA, coste.moneda());
        }
        this.costeEnvio = coste;
        this.estado = EstadoPedido.CONFIRMADO;
    }

    public int numBultos() {
        return lineas.stream().mapToInt(LineaPedido::cantidad).sum();
    }

    public BigDecimal pesoTotalKg() {
        return lineas.stream()
                .map(LineaPedido::pesoTotalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void marcarErrorEnvio() {
        exigirEstadoConfirmando(EstadoPedido.ERROR_ENVIO);
        this.estado = EstadoPedido.ERROR_ENVIO;
    }

    public PedidoId id() {
        return id;
    }

    public ClienteId clienteId() {
        return clienteId;
    }

    public String codigoPostalDestino() {
        return codigoPostalDestino;
    }

    public List<LineaPedido> lineas() {
        return lineas;
    }

    public Instant fecha() {
        return fecha;
    }

    public EstadoPedido estado() {
        return estado;
    }

    private void exigirEstadoConfirmando(EstadoPedido destino) {
        if (estado != EstadoPedido.CONFIRMANDO) {
            throw new TransicionEstadoInvalidaException(estado, destino);
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Pedido otro && id.equals(otro.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
