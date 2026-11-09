package br.edu.biblioteca.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${app.cors.origens:http://localhost:5173}")
    private String[] origensPermitidas;

    /** Relógio injetável: permite testar regras de prazo sem depender da data real. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Libera o frontend React (Vite, porta 5173) para chamar a API. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }
}
