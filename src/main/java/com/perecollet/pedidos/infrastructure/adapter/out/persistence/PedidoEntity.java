package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.domain.model.EstadoPedido;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedido")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class PedidoEntity {

    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "codigo_postal_destino", nullable = false, length = 10)
    private String codigoPostalDestino;

    @Column(nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "importe_productos", nullable = false, precision = 12, scale = 2)
    private BigDecimal importeProductos;

    @Column(name = "coste_envio", precision = 12, scale = 2)
    private BigDecimal costeEnvio;

    @Column(name = "importe_total", precision = 12, scale = 2)
    private BigDecimal importeTotal;

    @ElementCollection
    @CollectionTable(name = "linea_pedido", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderColumn(name = "posicion")
    private List<LineaPedidoEmbeddable> lineas = new ArrayList<>();
}