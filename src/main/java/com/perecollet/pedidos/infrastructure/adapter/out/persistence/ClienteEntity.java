package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "cliente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ClienteEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String email;
}