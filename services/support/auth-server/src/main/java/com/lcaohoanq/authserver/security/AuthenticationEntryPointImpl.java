
package com.lcaohoanq.authserver.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcaohoanq.commonlibrary.apis.MyApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    response.setContentType("application/json");
    response.setStatus(HttpStatus.UNAUTHORIZED.value());

    MyApiResponse<Object> errorResponse =
        new MyApiResponse.Error<>(
            HttpStatus.UNAUTHORIZED.value(),
            "Authentication Required",
            ex.getMessage(),
            request.getRequestURI(),
            Instant.now());

    objectMapper.writeValue(response.getOutputStream(), errorResponse);
  }
}
