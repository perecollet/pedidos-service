package com.perecollet.pedidos.infrastructure.adapter.out.logistica;

import java.math.BigDecimal;

public record TarifaResponse(BigDecimal coste, String moneda, Integer plazoDias) { }

