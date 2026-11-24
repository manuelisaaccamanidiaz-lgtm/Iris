package com.iris.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.iris.application.auth.CrearAdministradorInicial;

/**
 * Se ejecuta UNA vez cada que la app termina de arrancar (ApplicationRunner).
 * Si el .env trae IRIS_ADMIN_EMAIL e IRIS_ADMIN_PASSWORD, crea ese admin si aún no existe.
 */
@Component
class AdminInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);

    private final CrearAdministradorInicial crearAdmin;
    private final String nombre;
    private final String email;
    private final String password;

    AdminInicialRunner(CrearAdministradorInicial crearAdmin,
                       @Value("${iris.admin.nombre}") String nombre,
                       @Value("${iris.admin.email}") String email,
                       @Value("${iris.admin.password}") String password) {
        this.crearAdmin = crearAdmin;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("Sin IRIS_ADMIN_EMAIL/IRIS_ADMIN_PASSWORD: no se crea administrador inicial");
            return;
        }
        boolean creado = crearAdmin.ejecutar(nombre, email, password);
        // Nunca escribimos la contraseña en el log
        log.info(creado ? "Administrador inicial creado: {}" : "Administrador inicial ya existía: {}", email);
    }
}
