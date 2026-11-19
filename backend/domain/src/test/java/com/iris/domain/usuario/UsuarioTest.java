package com.iris.domain.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test 
    void rolSiempreOrganizador() {
        Email email = new Email("marcos@iris.com");

        Usuario usuario = Usuario.registrar("marcos", email, "hash-falso");

        assertEquals(Rol.ORGANIZADOR, usuario.rol());
    }
}
