package com.perecollet.pedidos;

import org.springframework.boot.SpringApplication;

public class TestPedidosServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(PedidosServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
