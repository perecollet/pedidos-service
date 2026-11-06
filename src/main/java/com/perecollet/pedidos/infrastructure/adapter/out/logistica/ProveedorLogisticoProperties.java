package com.perecollet.pedidos.infrastructure.adapter.out.logistica;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("proveedor-logistico")
public record ProveedorLogisticoProperties(@NotBlank String baseUrl, @NotBlank String token,
                                           Duration connectTimeout, Duration readTimeout) {

    @Override
    public String toString() {
        return "ProveedorLogisticoProperties[baseUrl=%s, token=****, connectTimeout=%s, readTimeout=%s]"
                .formatted(baseUrl, connectTimeout, readTimeout);
    }
}
