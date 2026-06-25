package com.hmbrandt.delay_tracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Delay tracker Service API")
                        .version("1.0")
                        .description("API for track job delays")
                        .contact(new Contact().name("Carlos Ramirez").email("ckarlos_perez@hotmail.com")));
    }
}