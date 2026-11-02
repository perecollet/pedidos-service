package com.perecollet.pedidos.domain.model;

import com.perecollet.pedidos.domain.exception.MonedaNoCoincideException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Dinero(BigDecimal importe, Currency moneda) {

    private static final Currency EUR = Currency.getInstance("EUR");

    public Dinero {

        Objects.requireNonNull(importe, "importe");
        Objects.requireNonNull(moneda, "moneda");

        if (importe.signum() < 0) {
            throw new IllegalArgumentException("El importe no puede ser menor que 0: " + importe);
        }

        importe = importe.setScale(moneda.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
    }

    public static Dinero of(BigDecimal importe, Currency moneda) {
        return new Dinero(importe, moneda);
    }

    public static Dinero euros(BigDecimal importe) {
        return new Dinero(importe, EUR);
    }

    public static Dinero cero(Currency moneda) {
        return new Dinero(BigDecimal.ZERO, moneda);
    }

    public Dinero sumar(Dinero otro) {
        if (!this.moneda.equals(otro.moneda)) {
            throw new MonedaNoCoincideException(this.moneda, otro.moneda);
        }
        return new Dinero(importe.add(otro.importe), this.moneda);
    }

    public Dinero multiplicar(int multiplicador) {
        if (multiplicador < 0) {
            throw new IllegalArgumentException("El multiplicador no puede ser negativo: " + multiplicador);
        }
        return new Dinero(importe.multiply(BigDecimal.valueOf(multiplicador)), moneda);
    }
}
