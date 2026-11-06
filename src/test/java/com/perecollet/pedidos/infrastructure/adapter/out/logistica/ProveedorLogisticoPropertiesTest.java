package com.perecollet.pedidos.infrastructure.adapter.out.logistica;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ProveedorLogisticoPropertiesTest {

    @EnableConfigurationProperties(ProveedorLogisticoProperties.class)
    private static class Configuracion {
    }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(Configuracion.class);

    @Test
    void givenProperties_whenToString_thenNoMuestraElTokenPeroSiElResto() {
        ProveedorLogisticoProperties propiedades = new ProveedorLogisticoProperties(
                "http://proveedor", "token-secreto", Duration.ofSeconds(2), Duration.ofSeconds(6));

        assertThat(propiedades.toString())
                .doesNotContain("token-secreto")
                .contains("http://proveedor");
    }

    @Test
    void givenBaseUrlYToken_whenArrancar_thenElContextoLevanta() {
        contexto.withPropertyValues("proveedor-logistico.base-url=http://proveedor", "proveedor-logistico.token=abc")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void givenTokenEnBlanco_whenArrancar_thenElContextoFalla(String token) {
        contexto.withPropertyValues("proveedor-logistico.base-url=http://proveedor", "proveedor-logistico.token=" + token)
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void givenBaseUrlEnBlanco_whenArrancar_thenElContextoFalla() {
        contexto.withPropertyValues("proveedor-logistico.base-url=", "proveedor-logistico.token=abc")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
