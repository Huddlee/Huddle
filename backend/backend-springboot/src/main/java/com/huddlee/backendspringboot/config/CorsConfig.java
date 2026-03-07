package com.huddlee.backendspringboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*") // Use patterns instead of allowedOrigins("*")
                        .allowedMethods("*")        // Allow ALL methods (including PATCH, etc.)
                        .allowedHeaders("*")        // Allow ALL headers
                        .allowCredentials(true);    // Allow cookies/auth tokens
            }
        };
    }
}