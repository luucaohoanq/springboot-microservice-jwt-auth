package com.lcaohoanq.authserver.filter;

import com.lcaohoanq.authserver.components.JwtTokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtils jwtTokenUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                if (jwtTokenUtils.validateToken(token)) {
                    // Extract user information from token
                    String username = jwtTokenUtils.extractUsername(token);
                    String role = jwtTokenUtils.extractRole(token);
                    Long userId = jwtTokenUtils.extractUserId(token);
                    String email = jwtTokenUtils.extractEmail(token);
                    
                    // Create authentication token
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    UsernamePasswordAuthenticationToken authenticationToken = 
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                    
                    // Add user details to the authentication token
                    authenticationToken.setDetails(new UserDetails(userId, username, email, role));
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    
                    log.debug("Successfully authenticated user: {} with role: {}", username, role);
                }
            } catch (Exception e) {
                log.error("JWT authentication error: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        
        filterChain.doFilter(request, response);
    }

    // Inner class to hold user details
    public record UserDetails(Long userId, String username, String email, String role) {

    }
}