package com.perecollet.pedidos.infrastructure.adapter.out.logistica;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("proveedor-logistico")
public record ProveedorLogisticoProperties(String baseUrl, String token, Duration connectTimeout, Duration readTimeout) { }
