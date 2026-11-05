package com.perecollet.pedidos.infrastructure.adapter.out.logistica;

import java.math.BigDecimal;

public record TarifaRequest(String codigoPostalDestino, BigDecimal pesoKg, int numBultos) { }
