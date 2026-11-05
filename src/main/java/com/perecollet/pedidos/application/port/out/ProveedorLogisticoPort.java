package com.perecollet.pedidos.application.port.out;

import com.perecollet.pedidos.domain.model.Dinero;

import java.math.BigDecimal;

public interface ProveedorLogisticoPort {

    Dinero obtenerTarifa(String codigoPostalDestino, BigDecimal pesoKg, int numBultos);
}
