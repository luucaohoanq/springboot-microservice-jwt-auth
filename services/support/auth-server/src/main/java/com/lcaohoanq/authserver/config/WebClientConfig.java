package com.lcaohoanq.authserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${geo.api.base-url}")
    private String geoApiBaseUrl;

    @Bean
    public WebClient geoWebClient() {
        return WebClient.builder()
            .baseUrl(geoApiBaseUrl)
            .build();
    }
}
