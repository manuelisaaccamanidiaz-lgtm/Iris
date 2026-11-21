package com.iris.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Enciende @Async y define la "cocina": el pool de hilos que indexa documentos. */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    ThreadPoolTaskExecutor indexadorExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(2);              // 2 "cocineros": cuidamos la cuota gratis de Gemini
        pool.setMaxPoolSize(2);
        pool.setQueueCapacity(100);           // hasta 100 documentos esperando turno
        pool.setThreadNamePrefix("indexador-");
        // Al apagar la app, espera hasta 30 s a que terminen los que están en curso
        pool.setWaitForTasksToCompleteOnShutdown(true);
        pool.setAwaitTerminationSeconds(30);
        pool.initialize();
        return pool;
    }
}
