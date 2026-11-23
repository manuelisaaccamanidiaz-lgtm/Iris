package com.iris.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Día 4.3. @EnableScheduling enciende el "despertador" (@Scheduled), igual que @EnableAsync
 * encendió @Async en el Día 2. Y un pool propio para el correo: enviar por SMTP tarda,
 * y la petición HTTP no debe quedarse esperando.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(CorreoPropiedades.class)
public class NotificacionesConfig {

    @Bean
    ThreadPoolTaskExecutor correoExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(1);              // un "cartero" basta, y respeta los límites de Gmail
        pool.setMaxPoolSize(1);
        pool.setQueueCapacity(200);
        pool.setThreadNamePrefix("correo-");
        pool.initialize();
        return pool;
    }
}
