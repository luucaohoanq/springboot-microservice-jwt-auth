package com.lcaohoanq.productservice.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
public class OpenAPIConfig {

  private final Environment env;

  @Bean
  public OpenAPI customOpenAPI() {
    String applicationName = env.getProperty("spring.application.name");

    return new OpenAPI()
        .info(
            new Info()
                .title(String.format("%s API", applicationName))
                .version("1.0")
                .description(String.format("API documentation for %s", applicationName))
                .contact(new Contact().name(String.format("Team %s", applicationName))))
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
