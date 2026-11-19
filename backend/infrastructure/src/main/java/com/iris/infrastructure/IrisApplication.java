package com.iris.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de Iris. Vive en infrastructure porque Spring Boot es un
 * detalle técnico: el dominio y los casos de uso no saben que existe.
 */
@SpringBootApplication
public class IrisApplication {

    public static void main(String[] args) {
        SpringApplication.run(IrisApplication.class, args);
    }
}
