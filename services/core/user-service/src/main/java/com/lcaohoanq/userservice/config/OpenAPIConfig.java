package com.lcaohoanq.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

  @Bean
  public OpenAPI customOpenAPI() {

    return new OpenAPI()
        .info(
            new Info()
                .title("User Service API")
                .version("1.0")
                .description("API Documentation for User Service")
                .contact(new Contact().name("User Service Team")))
        .externalDocs(
            new ExternalDocumentation()
                .description("Additional Documentation")
                .url("https://example.com"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearer-key",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
