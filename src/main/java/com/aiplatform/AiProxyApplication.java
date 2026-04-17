package com.aiplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación AI Proxy Platform.
 *
 * @EnableScheduling activa el soporte para tareas programadas (@Scheduled),
 * necesario para los resets automáticos de rate limit y cuota mensual.
 */
@SpringBootApplication
@EnableScheduling
public class AiProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiProxyApplication.class, args);
    }
}
