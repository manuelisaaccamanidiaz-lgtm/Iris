package com.iris.infrastructure.adapter.out.seguridad;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.CodificadorPassword;

/** Adaptador: implementa el puerto CodificadorPassword usando el BCrypt de Spring Security. */
@Component
class BCryptCodificadorPassword implements CodificadorPassword {

    private final PasswordEncoder passwordEncoder;

    BCryptCodificadorPassword(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String codificar(String passwordPlano) {
        return passwordEncoder.encode(passwordPlano); // genera sal nueva cada vez
    }

    @Override
    public boolean coincide(String passwordPlano, String hashGuardado) {
        return passwordEncoder.matches(passwordPlano, hashGuardado); // saca la sal del hash y compara
    }
}
