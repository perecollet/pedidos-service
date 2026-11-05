package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class LineaPedidoEmbeddable {

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "peso_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal pesoKg;
}