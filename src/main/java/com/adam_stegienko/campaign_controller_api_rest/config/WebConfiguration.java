package com.adam_stegienko.campaign_controller_api_rest.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration {

    @Autowired
    private Environment env;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                // Check if the 'dev' profile is active
                if (Arrays.asList(env.getActiveProfiles()).contains("local")) {
                    registry.addMapping("/v1/api/**")
                            .allowedOrigins("http://localhost:3000", "http://10.74.0.123:3000", "https://campaign-controller.apps.stegienko.local")
                            .allowedMethods("*")
                            .allowedHeaders("*")
                            .allowCredentials(true);
                } else if (Arrays.asList(env.getActiveProfiles()).contains("dev")) {
                    registry.addMapping("/v1/api/**")
                            .allowedOrigins("https://campaign-controller-dev.apps.stegienko.local")
                            .allowedMethods("*")
                            .allowedHeaders("*")
                            .allowCredentials(true);
                } else if (Arrays.asList(env.getActiveProfiles()).contains("stage")) {
                    registry.addMapping("/v1/api/**")
                            .allowedOrigins("https://campaign-controller-stage.apps.stegienko.com")
                            .allowedMethods("*")
                            .allowedHeaders("*")
                            .allowCredentials(true);
                }
            }
        };
    }
}
