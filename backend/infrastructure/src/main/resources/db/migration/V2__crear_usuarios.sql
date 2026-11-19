-- Organizadores (y administradores) de Iris.
CREATE TABLE usuarios (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre         VARCHAR(120) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    -- Nunca la contraseña: solo su hash BCrypt (60 caracteres; dejamos margen)
    password_hash  VARCHAR(100) NOT NULL,
    rol            VARCHAR(20)  NOT NULL DEFAULT 'ORGANIZADOR',
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ORGANIZADOR', 'ADMIN'))
);

-- Email único sin importar mayúsculas: Ana@x.com y ana@x.com son la misma cuenta
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (lower(email));
