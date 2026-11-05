package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "producto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ProductoEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "peso_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal pesoKg;
}