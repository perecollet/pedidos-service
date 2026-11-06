package com.perecollet.pedidos.domain.model;

import com.perecollet.pedidos.domain.exception.MonedaNoCoincideException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DineroTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    private static Dinero eur(String importe) {
        return Dinero.euros(new BigDecimal(importe));
    }

    private static Dinero usd(String importe) {
        return Dinero.of(new BigDecimal(importe), USD);
    }

    // --- Creación ---

    @Test
    void givenImporteYMoneda_whenOf_thenGuardaImporteConEscalaDos() {
        Dinero dinero = Dinero.of(new BigDecimal("7.9"), EUR);

        assertThat(dinero.importe()).isEqualTo(new BigDecimal("7.90"));
        assertThat(dinero.moneda()).isEqualTo(EUR);
    }

    @Test
    void givenImporte_whenEuros_thenCreaDineroEnEur() {
        Dinero dinero = Dinero.euros(new BigDecimal("19.99"));

        assertThat(dinero.importe()).isEqualTo(new BigDecimal("19.99"));
        assertThat(dinero.moneda()).isEqualTo(EUR);
    }

    @Test
    void givenMoneda_whenCero_thenImporteEsCeroConEscalaDos() {
        Dinero cero = Dinero.cero(EUR);

        assertThat(cero.importe()).isEqualTo(new BigDecimal("0.00"));
        assertThat(cero.moneda()).isEqualTo(EUR);
    }

    @ParameterizedTest
    @CsvSource({
            "7.945, EUR, 7.94",     // empate: redondea al par
            "7.955, EUR, 7.96",     // empate: redondea al par
            "7.946, EUR, 7.95",     // sin empate
            "1234.5, JPY, 1234",    // 0 decimales
            "1235.5, JPY, 1236",    // 0 decimales
            "1.2345, KWD, 1.234",   // 3 decimales
            "1.2355, KWD, 1.236"    // 3 decimales
    })
    void givenImporteConMasDecimalesQueLaMoneda_whenOf_thenRedondeaHalfEvenALaEscalaDeLaMoneda(
            String importe, String moneda, String esperado) {
        Dinero dinero = Dinero.of(new BigDecimal(importe), Currency.getInstance(moneda));

        // isEqualTo y no compareTo: la escala también forma parte de lo que se comprueba
        assertThat(dinero.importe()).isEqualTo(new BigDecimal(esperado));
    }

    @Test
    void givenImporteNulo_whenOf_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> Dinero.of(null, EUR))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenMonedaNula_whenOf_thenLanzaNullPointerException() {
        assertThatThrownBy(() -> Dinero.of(BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void givenImporteNegativo_whenOf_thenLanzaIllegalArgumentException() {
        BigDecimal negativo = new BigDecimal("-0.01");

        assertThatThrownBy(() -> Dinero.of(negativo, EUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Sumar ---

    @Test
    void givenDosImportesEnEur_whenSumar_thenDevuelveLaSuma() {
        Dinero resultado = eur("10.50").sumar(eur("7.95"));

        assertThat(resultado).isEqualTo(eur("18.45"));
    }

    @Test
    void givenImportesQueEnDoubleSonInexactos_whenSumar_thenResultadoEsExacto() {
        // 0.1 + 0.2 en double da 0.30000000000000004
        Dinero resultado = eur("0.10").sumar(eur("0.20"));

        assertThat(resultado.importe()).isEqualTo(new BigDecimal("0.30"));
    }

    @Test
    void givenDosImportes_whenSumar_thenNoModificaLosOriginales() {
        Dinero a = eur("10.00");
        Dinero b = eur("5.00");

        a.sumar(b);

        assertThat(a).isEqualTo(eur("10.00"));
        assertThat(b).isEqualTo(eur("5.00"));
    }

    @Test
    void givenMonedasDistintas_whenSumar_thenLanzaMonedaNoCoincideException() {
        Dinero euros = eur("10.00");
        Dinero dolares = usd("10.00");

        assertThatThrownBy(() -> euros.sumar(dolares))
                .isInstanceOf(MonedaNoCoincideException.class);
    }

    // --- Multiplicar ---

    @Test
    void givenImporteYCantidad_whenMultiplicar_thenDevuelveElProducto() {
        Dinero resultado = eur("19.99").multiplicar(3);

        assertThat(resultado).isEqualTo(eur("59.97"));
    }

    @Test
    void givenCantidadCero_whenMultiplicar_thenDevuelveCero() {
        Dinero resultado = eur("19.99").multiplicar(0);

        assertThat(resultado).isEqualTo(Dinero.cero(EUR));
    }

    @Test
    void givenCantidadNegativa_whenMultiplicar_thenLanzaIllegalArgumentException() {
        Dinero dinero = eur("19.99");

        assertThatThrownBy(() -> dinero.multiplicar(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Igualdad (value object) ---

    @Test
    void givenMismoImporteConDistintaEscala_whenComparar_thenSonIguales() {
        Dinero a = eur("7.9");
        Dinero b = eur("7.90");

        assertThat(a)
                .isEqualTo(b)
                .hasSameHashCodeAs(b);
    }

    @Test
    void givenMismoImporteYDistintaMoneda_whenComparar_thenNoSonIguales() {
        assertThat(eur("10.00")).isNotEqualTo(usd("10.00"));
    }
}