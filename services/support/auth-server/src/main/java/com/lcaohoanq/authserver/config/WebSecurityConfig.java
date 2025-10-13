package com.lcaohoanq.authserver.config;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
@EnableWebMvc
public class WebSecurityConfig {

  private final AuthenticationEntryPoint authenticationEntryPoint;
  private final AccessDeniedHandler accessDeniedHandler;

  @Value("${api.prefix}")
  private String apiPrefix;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            auth ->
                auth
                    // === PUBLIC ENDPOINTS (No authentication required) ===

                    // Public endpoints
                    .requestMatchers(
                        String.format("%s/auth/login", apiPrefix),
                        String.format("%s/auth/logout", apiPrefix),
                        String.format("%s/auth/validate", apiPrefix),
                        String.format("%s/auth/activate", apiPrefix),
                        String.format("%s/auth/refresh", apiPrefix),
                        String.format("%s/auth/register", apiPrefix),
                        String.format("%s/auth/reset-password/**", apiPrefix),
                        String.format("%s/public/**", apiPrefix))
                    .permitAll()

                    // Swagger UI and API docs
                    .requestMatchers(
                        apiPrefix + "/performance/**",
                        "/graphiql",
                        "/graphql",
                        "/error",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/v3/api-docs/swagger-config",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        apiPrefix + "/swagger-ui/**",
                        apiPrefix + "/swagger-ui.html",
                        apiPrefix + "/api-docs/**",
                        "/custom-swagger-ui/**",
                        "/actuator/**")
                    .permitAll()

                    // All other endpoints require authentication
                    .anyRequest()
                    .authenticated());


    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allow your Vercel domain and localhost for development
    configuration.setAllowedOrigins(
        Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000",
            "https://orchid-project-vert.vercel.app",
            "https://vercel.com/lcaohoanqs-projects/orchid-project/5htCDU8U65i2NjaypLCaXibFVVf1"));

    // Allow common HTTP methods
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

    // Allow all headers
    configuration.setAllowedHeaders(
        Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"));

    configuration.setAllowCredentials(true);
    configuration.setExposedHeaders(
        Arrays.asList(
            "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials", "Authorization"));
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
