package com.iris.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra las propiedades del antivirus (mismo patrón que NotificacionesConfig con el correo). */
@Configuration
@EnableConfigurationProperties(AntivirusPropiedades.class)
public class AntivirusConfig {
}
