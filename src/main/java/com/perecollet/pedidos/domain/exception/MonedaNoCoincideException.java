package com.perecollet.pedidos.domain.exception;

import java.util.Currency;

public class MonedaNoCoincideException extends RuntimeException {

    private static final String MESSAGE = "No se puede operar %s con %s";
    public MonedaNoCoincideException(Currency esperada, Currency recibida) {
        super(MESSAGE.formatted(esperada, recibida));
    }
}
