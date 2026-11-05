package com.perecollet.pedidos.infrastructure.adapter.out.persistence;

import com.perecollet.pedidos.TestcontainersConfiguration;
import com.perecollet.pedidos.domain.model.Dinero;
import com.perecollet.pedidos.domain.model.Producto;
import com.perecollet.pedidos.domain.model.ProductoId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.perecollet.pedidos.infrastructure.adapter.out.persistence.DatosSeed.RATON;
import static com.perecollet.pedidos.infrastructure.adapter.out.persistence.DatosSeed.TECLADO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProductoPersistenceAdapterTest {

    @Autowired
    private ProductoPersistenceAdapter adapter;

    @Test
    void givenIdsDelSeed_whenBuscarPorIds_thenDevuelveLosProductosConPrecioYPeso() {
        Map<ProductoId, Producto> encontrados = adapter.buscarPorIds(Set.of(TECLADO, RATON));

        assertThat(encontrados).containsOnlyKeys(TECLADO, RATON);
        Producto teclado = encontrados.get(TECLADO);
        assertThat(teclado.nombre()).isEqualTo("Teclado mecánico");
        assertThat(teclado.precio()).isEqualTo(Dinero.euros(new BigDecimal("49.99")));
        assertThat(teclado.pesoKg()).isEqualByComparingTo("1.25");
    }

    @Test
    void givenUnIdInexistente_whenBuscarPorIds_thenNoApareceEnElMapa() {
        ProductoId inexistente = new ProductoId(UUID.randomUUID());

        Map<ProductoId, Producto> encontrados = adapter.buscarPorIds(Set.of(TECLADO, inexistente));

        assertThat(encontrados).containsOnlyKeys(TECLADO);
    }

    @Test
    void givenConjuntoVacio_whenBuscarPorIds_thenDevuelveMapaVacio() {
        assertThat(adapter.buscarPorIds(Set.of())).isEmpty();
    }
}