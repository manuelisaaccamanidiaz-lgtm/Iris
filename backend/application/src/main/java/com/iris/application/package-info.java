/**
 * Capa de aplicación de Iris: los casos de uso.
 *
 * <p>Cada caso de uso (RegistrarUsuario, IniciarSesion, ConsultarDocumentos...)
 * orquesta el dominio usando sus puertos, sin saber si detrás hay PostgreSQL,
 * Groq o Gemini. Depende solo del módulo domain.
 */
package com.iris.application;
